(ns com.yetanalytics.lrs-admin-ui.handlers
  (:require [re-frame.core                                    :as re-frame]
            [reagent.core                                     :as r]
            [com.yetanalytics.lrs-admin-ui.db                 :as db]
            [com.yetanalytics.lrs-admin-ui.input              :as input]
            [day8.re-frame.http-fx]
            [com.yetanalytics.lrs-admin-ui.functions          :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.download :as download]
            [com.yetanalytics.lrs-admin-ui.functions.http     :as httpfn]
            [com.yetanalytics.lrs-admin-ui.functions.storage  :as stor]
            [com.yetanalytics.lrs-admin-ui.functions.password :as pass]
            [com.yetanalytics.lrs-admin-ui.functions.oidc     :as oidc]
            [com.yetanalytics.lrs-admin-ui.functions.time     :as t]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.re-oidc                         :as re-oidc]
            [ajax.core                                        :as ajax]
            [cljs.spec.alpha                                  :refer [valid?]]
            [clojure.string                                   :refer [split]]
            [goog.string                                      :refer [format]]
            goog.string.format
            [clojure.walk                                     :as w]
            [com.yetanalytics.lrs-admin-ui.spec.reaction      :as rs]
            [com.yetanalytics.lrs-admin-ui.spec.reaction-edit :as rse]
            [com.yetanalytics.lrs-admin-ui.language           :as lang]
            [com.yetanalytics.lrs-reactions.path              :as rpath]))

(def interaction-interceptor
  "Update `::db/last-interaction-time` to the current time whenever a change is
   made to the specified paths to the app-db. This is a heuristic that will
   detect some, but not all, user interactions with the UI."
  (re-frame/on-changes
   (fn [& _] (.now js/Date))
   [::db/last-interaction-time]
   ;; Places in app-db to detect changes
   [::db/session :page]
   [::db/login]
   [::db/credentials]
   [::db/accounts]
   [::db/new-account]
   [::db/browser]
   [::db/status]
   [::db/reactions]
   [::db/reaction-focus]
   [::db/editing-reaction]
   [::db/editing-reaction-template-json]))

(re-frame/reg-global-interceptor
 db/check-spec-interceptor)

(re-frame/reg-global-interceptor
 interaction-interceptor)

(re-frame/reg-event-fx
 :db/init
 (fn [_  [_ server-host]]
   {:db {::db/session {:page :credentials
                       :token (stor/get-item "lrs-jwt")
                       :username (stor/get-item "username")}
         ::db/login {:username nil
                     :password nil}
         ::db/credentials []
         ::db/accounts []
         ::db/new-account {:username nil
                           :password nil}
         ::db/update-password {:old-password nil
                               :new-password nil}
         ::db/browser {:content    nil
                       :address    nil
                       :credential nil
                       :more-link  nil
                       :batch-size 10
                       :back-stack []}
         ::db/server-host (or server-host "")
         ::db/xapi-prefix "/xapi"
         ::db/proxy-path (stor/get-item "proxy-path")
         ::db/language lang/language
         ::db/pref-lang :en-US
         ::db/jwt-refresh-interval 3540
         ::db/jwt-interaction-window 600
         ::db/stmt-get-max 10
         ::db/enable-admin-delete-actor false
         ::db/notifications []
         ::db/oidc-auth false
         ::db/oidc-enable-local-admin false
         ::db/enable-admin-status false
         ::db/status {}
         ::db/enable-reactions false
         ::db/reactions []
         ::db/last-interaction-time (.now js/Date)}
    :fx [[:dispatch [:db/get-env]]]}))

(re-frame/reg-event-fx
 :db/get-env
 (fn [{{server-host ::db/server-host} :db} _]
   (let [path-parts (split js/window.location.pathname "/")]
     {:http-xhrio {:method          :get
                   ;; Check if this is prod or dev. If prod and at admin path
                   ;; then use "env" relative path to account for proxy. If dev
                   ;; use absolute.
                   :uri             (if (and (some #(= "admin" %) path-parts)
                                             (= server-host ""))
                                      "env"
                                      (str server-host "/admin/env"))
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:db/set-env]
                   :on-failure      [:server-error]}})))

(re-frame/reg-event-fx
 :db/set-env
 (fn [{:keys [db]} [_ {:keys        [jwt-refresh-interval
                                     jwt-interaction-window
                                     url-prefix
                                     proxy-path
                                     enable-admin-status
                                     enable-reactions
                                     no-val?
                                     no-val-logout-url
                                     enable-admin-delete-actor
                                     admin-language-code
                                     stmt-get-max
                                     custom-language]
                       ?oidc        :oidc
                       ?oidc-enable :oidc-enable-local-admin}]]
   (let [jwt-refresh-interval*    (* 1000 jwt-refresh-interval)
         jwt-interaction-window*  (* 1000 jwt-interaction-window)
         oidc-enable-local-admin? (or ?oidc-enable false)
         admin-lang-keyword       (keyword admin-language-code)
         language-map             (merge-with merge
                                              lang/language
                                              custom-language)]
     ;; TODO: Put env vars in their own map
     {:db (cond-> (assoc db
                         ::db/jwt-refresh-interval jwt-refresh-interval*
                         ::db/jwt-interaction-window jwt-interaction-window*
                         ::db/xapi-prefix url-prefix
                         ::db/proxy-path proxy-path
                         ::db/oidc-enable-local-admin oidc-enable-local-admin?
                         ::db/enable-admin-status enable-admin-status
                         ::db/enable-reactions enable-reactions
                         ::db/enable-admin-delete-actor enable-admin-delete-actor
                         ::db/stmt-get-max stmt-get-max
                         ::db/pref-lang admin-lang-keyword
                         ::db/language language-map)
            (and no-val?
                 (not-empty no-val-logout-url))
            (assoc ::db/no-val-logout-url no-val-logout-url))
      :fx (cond-> []
            ?oidc (conj [:dispatch [:oidc/init ?oidc]])
            no-val? (conj [:dispatch [:session/proxy-token-init]]))
      :session/store ["proxy-path" proxy-path]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login / Auth
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :session/proxy-token-init
 (fn [_ _]
   ;; In this mode the token will be overwritten, so just store something and
   ;; move on. For testing the feature, this placeholder token has "username",
   ;; "perms" array containing "ADMIN" perm, and "domain" as the issuer
   (let [placeholder-token "eyJhbGciOiJIUzI1NiJ9.eyJkb21haW4iOiJodHRwczovL3Vuc2VjdXJlLnlldGFuYWx5dGljcy5jb20vcmVhbG0iLCJwZXJtcyI6WyJBRE1JTiJdLCJ1c2VybmFtZSI6IkNMSUZGLkNBU0VZLjEyMzQ1Njc4OTAifQ.2gRn_tDFBfJx2RE0pgvPM4wH__RnHf1E9kjsNlkLrnQ"]
     {:fx [[:dispatch [:session/set-token placeholder-token]]]})))

(re-frame/reg-event-db
 :login/set-username
 (fn [db [_ username]]
   (assoc-in db [::db/login :username] username)))

(re-frame/reg-event-db
 :login/set-password
 (fn [db [_ password]]
   (assoc-in db [::db/login :password] password)))

(re-frame/reg-event-fx
 :session/authenticate
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path
        :as db} :db} _]
   {:http-xhrio {:method          :post
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/account/login"
                                   proxy-path)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          (::db/login db)
                 :on-success      [:login/success-handler]
                 :on-failure      [:login/error-handler]}}))

(re-frame/reg-event-fx
 :login/success-handler
 (fn [{:keys [db]} [_ {:keys [json-web-token]}]]
   (let [jwt-refresh-interval (::db/jwt-refresh-interval db)]
     {:fx [[:dispatch [:session/set-token json-web-token]]
           [:dispatch [:login/set-password nil]]
           [:dispatch [:login/set-username nil]]
           [:dispatch-later {:ms       jwt-refresh-interval
                             :dispatch [:login/try-renew]}]]})))

(re-frame/reg-event-fx
 :session/set-token
 (fn [{:keys [db]} [_ token & {:keys [store?]
                               :or   {store? true}}]]
   (cond-> {:db (assoc-in db [::db/session :token] token)
            :fx [[:dispatch [:session/get-me]]]}
     store? (assoc :session/store ["lrs-jwt" token]))))

(re-frame/reg-event-fx
 :session/clear-token
 (fn [{:keys [db]} [_ & {:keys [store?]
                         :or   {store? true}}]]
   (cond-> {:db (assoc-in db [::db/session :token] nil)}
     store? (assoc :session/store ["lrs-jwt" nil]))))

(re-frame/reg-event-fx
 :session/get-me
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path :as db} :db} _]
   (when (not (get db ::db/oidc-auth))
     {:http-xhrio {:method          :get
                   :uri             (httpfn/serv-uri
                                     server-host
                                     "/admin/me"
                                     proxy-path)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:session/me-success-handler]
                   :on-failure      [:login/error-handler]
                   :interceptors    [httpfn/add-jwt-interceptor]}})))

(re-frame/reg-event-fx
 :session/me-success-handler
 (fn [_ [_ {:keys [username]}]]
   {:fx [[:dispatch [:session/set-username username]]]}))

(re-frame/reg-event-fx
 :login/error-handler
 (fn [_ [_ {:keys [status] :as error}]]
   ;; For auth, if its badly formed or not authorized give a specific error,
   ;; otherwise default to typical server error notice handling
   (if (or (= status 401) (= status 400))
     {:fx [[:dispatch [:notification/notify true
                       "Please enter a valid username and password!"]]]}
     {:fx [[:dispatch [:server-error error]]]})))

(re-frame/reg-event-fx
 :session/set-username
 (fn [{:keys [db]} [_ username & {:keys [store?]
                                  :or   {store? true}}]]
   (cond-> {:db (assoc-in db [::db/session :username] username)}
     store? (assoc :session/store ["username" username]))))

(re-frame/reg-event-fx
 :session/clear-username
 (fn [{:keys [db]} [_ & {:keys [store?]
                         :or   {store? true}}]]
   (cond-> {:db (assoc-in db [::db/session :username] nil)}
     store? (assoc :session/store ["username" nil]))))

(re-frame/reg-fx
 :session/store
 (fn [[key value]]
   (if value
     (stor/set-item! key value)
     (stor/remove-item! key))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; General
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti page-fx
  "Given a set-page event query vector, adds any effects of moving to that page.
  Note that you can use overloads beyond just the page keyword in your methods."
  (fn [[_ page]]
    page))

(defmethod page-fx :default [_] [])

(re-frame/reg-event-fx
 :session/set-page
 (fn [{:keys [db]} [_ page :as qvec]]
   {:db (assoc-in db [::db/session :page] page)
    :fx (page-fx qvec)}))

(re-frame/reg-event-fx
 :server-error
 (fn [_ [_ {:keys [response status]}]]
   ;;extract the error and present it in a notification. If 401 or 0, log out.
   (let [err (get response "error"
                  ;; If no error is provided, pass status
                  (format "Unknown Error, status: %s" status))
         message (cond (= status 0)
                       "Could not connect to LRS!"
                       (and err (< (count err) 100))
                       (str "Error from server: " err)
                       :else
                       "An unexpected error has occured!")]
     {:fx (cond-> [[:dispatch [:notification/notify true message]]]
            (some #(= status %) [0 401])
            (merge [:dispatch [:session/set-token nil]]))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Logout + Renewal
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :session/logout
 (fn [{:keys [db]} _]
   (let [{server-host        ::db/server-host
          proxy-path         ::db/proxy-path
          ?no-val-logout-url ::db/no-val-logout-url} db]
     (cond
       ;; OIDC login
       (oidc/logged-in? db)
       {:fx [[:dispatch [:session/clear-token]]
             [:dispatch [:session/clear-username]]
             [:dispatch [::re-oidc/logout]]]}
       ;; Proxy JWT login
       ?no-val-logout-url
       {:fx [[:dispatch [:session/clear-token]]
             [:dispatch [:session/clear-username]]
             [:session/no-val-logout-redirect
              {:logout-url ?no-val-logout-url}]]}
       ;; Regular JWT login
       :else
       {:http-xhrio
        {:method          :post
         :uri             (httpfn/serv-uri
                           server-host
                           "/admin/account/logout"
                           proxy-path)
         :params          {}
         :format          (ajax/json-request-format)
         :response-format (ajax/json-response-format {:keywords? true})
         :on-success      [:session/logout-success-handler]
         :on-failure      [:session/logout-error-handler]
         :interceptors    [httpfn/add-jwt-interceptor]}}))))

(re-frame/reg-event-fx
 :session/logout-success-handler
 (fn [_ _]
   {:fx [[:dispatch [:session/clear-token]]
         [:dispatch [:session/clear-username]]
         [:dispatch [:notification/notify false "You have logged out."]]]}))

(re-frame/reg-event-fx
 :session/logout-error-handler
 (fn [_ _]
   {:fx [[:dispatch [:notification/notify true "Logout error occured."]]]}))

(re-frame/reg-fx
 :session/no-val-logout-redirect
 (fn [{:keys [logout-url]}]
   (set! (.-location js/window) logout-url)))

(re-frame/reg-event-fx
 :login/try-renew
 (fn [{:keys [db]}]
   (let [{int-window    ::db/jwt-interaction-window
          last-int-time ::db/last-interaction-time} db
         current-token (get-in db [::db/session :token])
         current-time  (.now js/Date)]
     (cond
       (nil? current-token) ; logged out or non-JWT login
       {}
       (< (- current-time int-window) last-int-time current-time)
       {:fx [[:dispatch [:login/renew]]]}
       :else
       {:fx [[:dispatch [:session/logout]]]}))))

(re-frame/reg-event-fx
 :login/renew
 (fn [{:keys [db]}]
   (let [{server-host ::db/server-host
          proxy-path  ::db/proxy-path} db]
     {:http-xhrio
      {:method          :get
       :uri             (httpfn/serv-uri
                         server-host
                         "/admin/account/renew"
                         proxy-path)
       :format          (ajax/json-request-format)
       :response-format (ajax/json-response-format {:keywords? true})
       :on-success      [:login/renew-success]
       :on-failure      [:login/renew-error]
       :interceptors    [httpfn/add-jwt-interceptor]}})))

(re-frame/reg-event-fx
 :login/renew-success
 (fn [{:keys [db]} [_ {:keys [json-web-token]}]]
   (let [jwt-refresh-interval (::db/jwt-refresh-interval db)]
     {:fx [[:dispatch [:session/set-token json-web-token]]
           [:dispatch-later {:ms jwt-refresh-interval
                             :dispatch [:login/try-renew]}]]})))

(re-frame/reg-event-fx
 :login/renew-error
 (fn [_ [_ {:keys [status] :as error}]]
   (if (= 401 status)
     {:fx [[:dispatch [:notification/notify true
                       "Congratulations on being SQL LRS's biggest fan!"]]]}
     {:fx [[:dispatch [:server-error error]]]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Notifications / Alert Bar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Unfortunately need a stateful custom Effect for hiding the alert after a bit.
;; 're-frame/dispatch-later' mostly works but if two notifications are too close
;; the first one's hide dispatch will prematurely close the second one since the
;; dispatches are unaware of each other
(defonce dispatch-timeouts (r/atom {}))

;; inspired by https://purelyfunctional.tv/guide/timeout-effect-in-re-frame/
(re-frame/reg-fx
 :debounce-dispatch-later
 (fn [{:keys [key dispatch ms]}]
   (when-some [existing (get @dispatch-timeouts key)]
     (js/clearTimeout existing)
     (swap! dispatch-timeouts dissoc key))
   (when (some? dispatch)
     (swap! dispatch-timeouts assoc key
            (js/setTimeout
             (fn []
               (re-frame/dispatch dispatch))
             ms)))))


(defn remove-notice
  "Given a notice id and a collection of notifications return the collection
  with all notices of that id removed"
  [notifications notice-id]
  (filterv (fn [{:keys [id] :as notification}]
             (when (not (= id notice-id))
               notification))
           notifications))

(re-frame/reg-event-fx
 :notification/notify
 (fn [{:keys [db]} [_ error msg]]
   (let [notice-id (hash msg)
         notifications (get db ::db/notifications)]
     ;; Remove identical notifications, add the new one and (re)set a timer
     {:db (assoc db ::db/notifications
                 (conj (remove-notice notifications notice-id)
                       {:id notice-id
                        :error? error
                        :msg msg}))
      ;;set a timer to clear it eventually
      :debounce-dispatch-later {:key notice-id
                                :ms 5000
                                :dispatch [:notification/hide notice-id]}})))

(re-frame/reg-event-fx
 :notification/hide
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db ::db/notifications
               (remove-notice (get db ::db/notifications) id))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Downloads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-fx
 :download-json
 (fn [[json-data json-data-name]]
   (download/download-json json-data json-data-name)))

(re-frame/reg-fx
 :download-edn
 (fn [[edn-data edn-data-name]]
   (download/download-edn edn-data edn-data-name)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Data Browser
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :browser/load-xapi
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path
        xapi-prefix ::db/xapi-prefix} :db} [_ {:keys [path params]}]]
   (let [xapi-url (httpfn/build-xapi-url
                   server-host xapi-prefix path params proxy-path)]
     {:dispatch   [:browser/set-address xapi-url]
      :http-xhrio {:method          :get
                   :uri             xapi-url
                   :response-format (ajax/json-response-format {:keywords? false})
                   :on-success      [:browser/load-stmts-success]
                   :on-failure      [:server-error]
                   :interceptors    [httpfn/req-xapi-interceptor]}})))

(re-frame/reg-event-db
 :browser/set-address
 (fn [db [_ address]]
   (assoc-in db [::db/browser :address] address)))

(re-frame/reg-event-db
 :browser/load-stmts-success
 (fn [db [_ {:strs [statements more]}]]
   (update-in db [::db/browser] assoc
              :content   statements
              :more-link more)))

(re-frame/reg-event-fx
 :browser/more
 (fn [{:keys [db]} _]
   ;; Convert more link into params and request new data.
   (let [more-params
         (httpfn/extract-params (get-in db [::db/browser :more-link]))
         address (get-in db [::db/browser :address])]
     ;; Push current address into stack
     {:db (update-in db [::db/browser :back-stack] conj address)
      :dispatch [:browser/load-xapi {:params more-params}]})))

(re-frame/reg-event-fx
 :browser/back
 (fn [{:keys [db]} _]
   ;; Pop most recent from stack
   (let [back-stack (get-in db [::db/browser :back-stack])
         back-params (httpfn/extract-params (peek back-stack))]
     {:db (update-in db [::db/browser :back-stack] pop)
      :dispatch [:browser/load-xapi {:params back-params}]})))

(re-frame/reg-event-fx
 :browser/add-filter
 (fn [{:keys [db]} [_ param-key param-value]]
   (let [address (get-in db [::db/browser :address])
         params (-> (httpfn/extract-params address)
                    (dissoc "from" "limit")
                    (assoc param-key param-value))]
     ;; Clear back-stack
     {:db (assoc-in db [::db/browser :back-stack] [])
      :dispatch [:browser/load-xapi {:params params}]})))

(re-frame/reg-event-fx
 :browser/clear-filters
 (fn [{:keys [db]} _]
   {;; Clear back-stack and reset query
    :db (assoc-in db [::db/browser :back-stack] [])
    :dispatch [:browser/load-xapi]}))

(re-frame/reg-event-fx
 :browser/update-credential
 (fn [{:keys [db]} [_ key]]
   (let [credential (first (filter #(= key (:api-key %))
                                   (::db/credentials db)))]
     (when credential
       ;; Clear backstack and limit and filters
       {:db (update-in db [::db/browser] assoc
                       :credential credential
                       :back-stack []
                       :batch-size 10)
        :dispatch [:browser/load-xapi]}))))

(re-frame/reg-event-fx
 :browser/refresh
 (fn [{:keys [db]} _]
   (when (get-in db [::db/browser :credential])
     ;; Clear backstack
     {:db (assoc-in db [::db/browser :back-stack] [])
      :dispatch [:browser/load-xapi]})))

(re-frame/reg-event-fx
 :browser/update-batch-size
 (fn [{:keys [db]} [_ batch-size]]
   ;; Clear from and limit
   (let [address (get-in db [::db/browser :address])
         params (-> (httpfn/extract-params address)
                    (dissoc "from" "limit"))]
     ;; update batch to new size and clear back-stack
     {:db (update-in db [::db/browser] assoc
                     :batch-size batch-size
                     :back-stack [])
      :dispatch [:browser/load-xapi {:params params}]})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Api Key Management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db
 :credentials/set-credentials
 (fn [db [_ credentials]]
   (assoc db ::db/credentials credentials)))

(re-frame/reg-event-fx
 :credentials/load-credentials
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} _]
   {:http-xhrio {:method          :get
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds"
                                   proxy-path)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/set-credentials]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-db
 :credentials/update-credential
 (fn [db [_ idx credential]]
   (assoc-in db [::db/credentials idx] credential)))

(re-frame/reg-event-fx
 :credentials/save-credential
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} [_ credential]]
   {:http-xhrio {:method          :put
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds"
                                   proxy-path)
                 :params          credential
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/save-success]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))


(re-frame/reg-event-fx
 :credentials/save-success
 (fn [_ [_ {:keys [api-key]}]]
   {:fx [[:dispatch [:credentials/load-credentials]]
         [:dispatch [:notification/notify false
                     (format "Updated credential with key: %s"
                             (fns/elide api-key 10))]]]}))

(re-frame/reg-event-fx
 :credentials/create-credential
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} [_ credential]]
   {:http-xhrio {:method          :post
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds"
                                   proxy-path)
                 :params          credential
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/create-success]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :credentials/create-success
 (fn [_ [_ {:keys [api-key]}]]
   {:fx [[:dispatch [:credentials/load-credentials]]
         [:dispatch [:notification/notify false
                     (format "Created credential with key: %s"
                             (fns/elide api-key 10))]]]}))

(re-frame/reg-event-fx
 :credentials/delete-credential
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} [_ credential]]
   {:http-xhrio {:method          :delete
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds"
                                   proxy-path)
                 :params          credential
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/delete-success]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :credentials/delete-success
 (fn [_ [_ {:keys [api-key]}]]
   {:fx [[:dispatch [:credentials/load-credentials]]
         [:dispatch [:notification/notify false
                     (format "Deleted credential with key: %s"
                             (fns/elide api-key 10))]]]}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Account Management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(re-frame/reg-event-fx
 :accounts/load-accounts
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} _]
   {:http-xhrio {:method          :get
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/account"
                                   proxy-path)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:accounts/set-accounts]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :accounts/delete-account
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} [_ {:keys [account-id username]}]]
   {:http-xhrio {:method          :delete
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/account"
                                   proxy-path)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          {:account-id account-id}
                 :format          (ajax/json-request-format)
                 :on-success      [:accounts/delete-success username]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :accounts/delete-success
 (fn [_ [_ username _]]
   {:fx [[:dispatch [:accounts/load-accounts]]
         [:dispatch [:notification/notify false
                     (format "Deleted account with username: %s" username)]]]}))

(re-frame/reg-event-db
 :accounts/set-accounts
 (fn [db [_ accounts]]
   (assoc db ::db/accounts accounts)))

(re-frame/reg-event-fx
 :accounts/create-account
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path
        :as db} :db} _]
   (let [{:keys [username] :as new-account}
         (::db/new-account db)]
     (if (valid? ::input/valid-new-account new-account)
       {:http-xhrio {:method          :post
                     :uri             (httpfn/serv-uri
                                       server-host
                                       "/admin/account/create"
                                       proxy-path)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :params          new-account
                     :format          (ajax/json-request-format)
                     :on-success      [:accounts/create-success username]
                     :on-failure      [:accounts/create-error]
                     :interceptors    [httpfn/add-jwt-interceptor]}}
       {:fx         [[:dispatch [:notification/notify true
                                 "Username or Password did not meet requirements."]]]}))))

(re-frame/reg-event-db
 :new-account/set-new-account
 (fn [db [_ new-account]]
   (assoc db ::db/new-account new-account)))

(re-frame/reg-event-fx
 :accounts/create-success
 (fn [_ [_ username _]]
   {:fx [[:dispatch [:accounts/load-accounts]]
         [:dispatch [:new-account/set-new-account
                     {:username nil
                      :password nil}]]
         [:dispatch [:notification/notify false
                     (format "Created account with username: %s" username)]]]}))

(re-frame/reg-event-fx
 :accounts/create-error
 (fn [_ [_ {:keys [status] :as error}]]
   ;; For account creation, if its malformed give a specific error,
   ;; otherwise default to typical server error notice handling
   (if (= status 400)
     {:fx [[:dispatch [:notification/notify true
                       "Please enter a valid username and password!"]]]}
     {:fx [[:dispatch [:server-error error]]]})))

(re-frame/reg-event-db
 :new-account/set-username
 (fn [db [_ username]]
   (assoc-in db [::db/new-account :username] username)))

(re-frame/reg-event-db
 :new-account/set-password
 (fn [db [_ password]]
   (assoc-in db [::db/new-account :password] password)))

(re-frame/reg-event-fx
 :new-account/generate-password
 (fn [_ _]
   {:dispatch [:new-account/set-password (pass/pass-gen 12)]}))

(re-frame/reg-event-db
 :update-password/set-old-password
 (fn [db [_ password]]
   (assoc-in db [::db/update-password :old-password] password)))

(re-frame/reg-event-db
 :update-password/set-new-password
 (fn [db [_ password]]
   (assoc-in db [::db/update-password :new-password] password)))

(re-frame/reg-event-db
 :update-password/clear
 (fn [db _]
   (assoc db ::db/update-password {:old-password nil
                                   :new-password nil})))

(re-frame/reg-event-fx
 :update-password/generate-password
 (fn [_ _]
   {:dispatch [:update-password/set-new-password (pass/pass-gen 12)]}))

(re-frame/reg-event-fx
 :update-password/update-password!
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path
        :as db} :db} _]
   (let [update-password (::db/update-password db)]
     (if (valid? ::input/valid-update-password update-password)
       {:http-xhrio {:method          :put
                     :uri             (httpfn/serv-uri
                                       server-host
                                       "/admin/account/password"
                                       proxy-path)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :params          update-password
                     :format          (ajax/json-request-format)
                     :on-success      [:update-password/update-success]
                     :on-failure      [:update-password/update-error]
                     :interceptors    [httpfn/add-jwt-interceptor]}}
       {:fx         [[:dispatch [:notification/notify true
                                 "New password did not meet requirements."]]]}))))

(re-frame/reg-event-fx
 :update-password/update-success
 (fn [_ _]
   {:fx [[:dispatch [:update-password/clear]]
         [:dispatch [:session/set-page :credentials]]
         [:dispatch [:notification/notify false
                     "Password updated."]]]}))

(re-frame/reg-event-fx
 :update-password/update-error
 (fn [_ _]
   {:fx [[:dispatch [:notification/notify true
                     "Password update failed. Please try again."]]]}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete Actor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(re-frame/reg-event-fx
 :delete-actor/delete-actor
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db}
      [_ actor-ifi]]
   {:fx [[:http-xhrio
          {:method          :delete
           :uri             (httpfn/serv-uri
                             server-host
                             "/admin/agents"
                             proxy-path)
           :params          {:actor-ifi actor-ifi}
           :format          (ajax/json-request-format)
           :response-format (ajax/json-response-format {:keywords? false})
           :on-success      [:delete-actor/delete-success actor-ifi]
           :on-failure      [:delete-actor/server-error actor-ifi]
           :interceptors    [httpfn/add-jwt-interceptor]}]]}))

(re-frame/reg-event-fx
 :delete-actor/delete-success
 (fn [_ [_ actor-ifi]]
   {:fx [[:dispatch [:notification/notify false (str "Successfully deleted " actor-ifi)]]
         [:dispatch [:browser/load-xapi]]]}))
(re-frame/reg-event-fx
 :delete-actor/server-error
 (fn [_ [_ actor-ifi _err]]
   {:fx [[:dispatch  [:notification/notify true  (str "Error when attempting to delete actor " actor-ifi)]]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OIDC Support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :oidc/init
 (fn [{:keys [db]} [_ remote-config]]
   (let [?search (not-empty js/window.location.search)]
     {:db (assoc db ::db/oidc-auth true)
      :dispatch-n
      (cond-> [[::re-oidc/init (oidc/init-config remote-config)]]
        ?search (conj [::re-oidc/login-callback
                       oidc/static-config
                       ?search]))})))

(re-frame/reg-fx
 :oidc/clear-search-fx
 (fn [_]
   (oidc/push-state js/window.location.pathname)))

(re-frame/reg-event-fx
 :oidc/login-success
 (fn [_ _]
   {:fx [[:oidc/clear-search-fx {}]]}))

(re-frame/reg-event-fx
 :oidc/user-loaded
 (fn [{:keys [db]} _]
   (if-let [{:keys [access-token]
             {:strs [sub]} :profile} (::re-oidc/user db)]
     {:fx [[:dispatch [:session/set-token access-token
                       :store? false]]
           [:dispatch [:session/set-username sub
                       :store? false]]
           [:dispatch [:login/set-password nil]]
           [:dispatch [:login/set-username nil]]]}
     {})))

(re-frame/reg-event-fx
 :oidc/user-unloaded
 (fn [_ _]
   {:fx [[:dispatch [:session/clear-token]]
         [:dispatch [:session/clear-username]]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Status Dashboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :status/get-data
 (fn [{{server-host      ::db/server-host
        proxy-path       ::db/proxy-path
        {:keys [params]} ::db/status
        :as              db}
       :db}
      [_ include]]
   {;; Set loading state
    :db (reduce
         (fn [db' vis-k]
           (assoc-in db' [::db/status :loading vis-k] true))
         db
         include)
    ;; make request
    :fx [[:http-xhrio
          {:method          :get
           :uri             (httpfn/serv-uri
                             server-host
                             "/admin/status"
                             proxy-path)
           :params          (reduce-kv
                             (fn [m k v]
                               (assoc m (name k) v))
                             {"include" include}
                             params)
           :format          (ajax/json-request-format)
           :response-format (ajax/json-response-format {:keywords? false})
           :on-success      [:status/set-data include]
           :on-failure      [:status/server-error include]
           :interceptors    [httpfn/add-jwt-interceptor]}]]}))

(re-frame/reg-event-fx
 :status/server-error
 (fn [{:keys [db]} [_ include error]]
   {:db (reduce
         (fn [db' k]
           (update-in db' [::db/status :loading] dissoc k))
         db
         include)
    :fx [[:dispatch
          [:server-error error]]]}))

(def status-dispatch-all
  (into []
        (for [status-query ["statement-count"
                            "actor-count"
                            "last-statement-stored"
                            "timeline"
                            "platform-frequency"]]
          [:dispatch [:status/get-data [status-query]]])))

(defmethod page-fx :status [_]
  status-dispatch-all)

(re-frame/reg-event-fx
 :status/get-all-data
 (fn [_ _]
   {:fx status-dispatch-all}))

(defn- coerce-status-data
  "Convert string keys in status data to keyword where appropriate."
  [status-data]
  (reduce-kv
   (fn [m k v]
     (assoc m
            (keyword k)
            (if (= k "timeline")
              (w/keywordize-keys v)
              v)))
   {}
   status-data))

(re-frame/reg-event-fx
 :status/set-data
 (fn [{:keys [db]} [_ include status-data]]
   {:db (reduce
         (fn [db' k]
           (update-in db'
                      [::db/status :loading]
                      dissoc
                      k))
         (update-in db [::db/status :data] merge (coerce-status-data status-data))
         include)}))

(def timeline-control-fx
  "Fx call that will refesh the timeline for a control change, debounced."
  [:debounce-dispatch-later
   {:key :status/timeline-control
    :dispatch [:status/get-data ["timeline"]]
    :ms 1000}])

(re-frame/reg-event-fx
 :status/set-timeline-unit
 (fn [{:keys [db]} [_ unit]]
   {:db (assoc-in db [::db/status :params :timeline-unit] unit)
    :fx [timeline-control-fx]}))

(re-frame/reg-event-fx
 :status/set-timeline-since
 (fn [{{{{:keys [timeline-until]
          :or   {timeline-until (t/timeline-until-default)}}
         :params} ::db/status
        :as       db} :db} [_ since-datetime-str]]
   (try
     (let [timeline-since (t/local-datetime->utc since-datetime-str)]
       (if (< (js/Date. timeline-since) (js/Date. timeline-until))
         {:db (assoc-in db
                        [::db/status :params :timeline-since]
                        timeline-since)
          :fx [timeline-control-fx]}
         (.log js/console
               "New timeline-since ignored, must be before timeline-until.")))
     (catch js/Error _
       (.log js/console
             (str "Invalid timestamp " since-datetime-str " was ignored"))))))

(re-frame/reg-event-fx
 :status/set-timeline-until
 (fn [{{{{:keys [timeline-since]
          :or   {timeline-since (t/timeline-since-default)}}
         :params} ::db/status
        :as       db} :db} [_ until-datetime-str]]
   (try
     (let [timeline-until (t/local-datetime->utc until-datetime-str)]
       (if (< (js/Date. timeline-since) (js/Date. timeline-until))
         {:db (assoc-in db
                        [::db/status :params :timeline-until]
                        timeline-until)
          :fx [timeline-control-fx]}
         (.log js/console
               "New timeline-until ignored, must be after timeline-since.")))
     (catch js/Error _
       (.log js/console
             (str "Invalid timestamp " until-datetime-str " was ignored"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reaction Management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod page-fx :reactions [_]
  [[:dispatch [:reaction/back-to-list]]
   [:dispatch [:reaction/load-reactions]]])

(re-frame/reg-event-fx
 :reaction/load-reactions
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} _]
   {:http-xhrio {:method          :get
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/reaction"
                                   proxy-path)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:reaction/set-reactions]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-db
 :reaction/set-reactions
 (fn [db [_ {:keys [reactions]}]]
   (assoc db
          ::db/reactions
          (mapv
           (fn [reaction]
             (update-in reaction [:ruleset :template] w/stringify-keys))
           reactions))))

(re-frame/reg-event-db
 :reaction/set-focus
 (fn [db [_ reaction-id]]
   (assoc db ::db/reaction-focus reaction-id)))

(re-frame/reg-event-db
 :reaction/unset-focus
 (fn [db _]
   (dissoc db ::db/reaction-focus)))

(defn- prep-edit-reaction-template
  [{reaction ::db/editing-reaction
    :as      db}]
  (assoc db
         ::db/editing-reaction-template-json
         (.stringify js/JSON
                     (clj->js (get-in reaction [:ruleset :template]))
                     nil
                     2)))

(defn- find-reaction
  [db reaction-id]
  (some
   (fn [{:keys [id] :as reaction}]
     (when (= id reaction-id)
       reaction))
   (::db/reactions db)))

;; TODO: Currently unimplemented
#_(re-frame/reg-event-fx
 :reaction/download-all
 (fn [{:keys [db]}]
   (let [reactions (->> (::db/reactions db)
                        (map rfns/index-conditions) ; sort by sort-idx, then remove
                        (map rfns/strip-condition-indices)
                        (mapv #(select-keys % [:title :ruleset :active])))]
     {:download-edn [reactions "reactions"]})))

(re-frame/reg-event-fx
 :reaction/download
 (fn [{:keys [db]} [_ reaction-id]]
   (if-let [reaction (some-> (find-reaction db reaction-id)
                             rfns/index-conditions ; sort by sort-idx, then remove
                             rfns/strip-condition-indices
                             (select-keys [:title :ruleset :active]))]
     {:download-edn [reaction (:title reaction)]}
     {:fx [[:dispatch [:notification/notify true
                       "Cannot download, reaction not found!"]]]})))

(re-frame/reg-event-fx
 :reaction/upload
 (fn [{:keys [db]}]
   {:db db}))

(re-frame/reg-event-fx
 :reaction/edit
 (fn [{:keys [db]} [_ reaction-id]]
   (if-let [reaction (some-> (find-reaction db reaction-id)
                             rfns/index-conditions)]
     {:db (-> db
              (assoc ::db/editing-reaction reaction)
              prep-edit-reaction-template)
      ;; unset focus in case we're looking at one
      :fx [[:dispatch [:reaction/unset-focus]]]}
     {:fx [[:dispatch [:notification/notify true
                       "Cannot edit, reaction not found!"]]]})))

(re-frame/reg-event-fx
 :reaction/new
 (fn [{:keys [db]} _]
   (let [reaction {:title  (format "reaction_%s"
                                   (fns/rand-alpha-str 8))
                   :active true
                   :ruleset
                   {:identityPaths [["actor" "mbox"]
                                    ["actor" "openid"]
                                    ["actor" "mbox_sha1sum"]
                                    ["actor" "account" "homePage"]
                                    ["actor" "account" "name"]]
                    :conditions    {}
                    :template      {"actor"
                                    {"name" "Actor Example",
                                     "mbox" "mailto:actor_example@yetanalytics.com"},
                                    "object" {"id" "https://www.yetanalytics.com/xapi/activities/example_activity"},
                                    "verb" {"id" "https://adlnet.gov/expapi/verbs/completed"}}}}]
     {:db (-> db
              (assoc ::db/editing-reaction reaction)
              prep-edit-reaction-template)
      ;; unset focus in case we're looking at one
      :fx [[:dispatch [:reaction/unset-focus]]]})))

(re-frame/reg-event-fx
 :reaction/upload-edit
 (fn [{:keys [db]} [_ upload-data]]
   (if-some [edn-data (try
                          (js->clj (js/JSON.parse upload-data)
                                   :keywordize-keys true)
                          (catch js/Error _ nil))]
     (let [reaction (-> edn-data
                        (select-keys [:title :ruleset :active])
                        (update-in [:ruleset :template] w/stringify-keys)
                        rfns/index-conditions)]
       (if (valid? ::rse/reaction reaction)
         {:db (-> db
                  (assoc ::db/editing-reaction reaction)
                  prep-edit-reaction-template)
          :fx [[:dispatch [:reaction/unset-focus]]]}
         {:fx [[:dispatch [:notification/notify true
                           "Cannot upload invalid reaction"]]]}))
     {:fx [[:dispatch [:notification/notify true
                       "Cannot upload invalid JSON data as reaction"]]]})))

(re-frame/reg-event-fx
 :reaction/revert-edit
 (fn [{:keys [db]} _]
   (when-let [reaction-id (get-in db [::db/editing-reaction :id])]
     (when-let [reaction (some-> (find-reaction db reaction-id)
                                 rfns/index-conditions)]
       {:db (-> db
                (assoc ::db/editing-reaction reaction)
                prep-edit-reaction-template)
        ;; unset focus in case we're looking at one
        :fx [[:dispatch [:reaction/unset-focus]]]}))))

(re-frame/reg-event-fx
 :reaction/server-error
 (fn [_ [_ {:keys [response status]}]]
   (if (= 400 status)
     {:fx [[:dispatch
            [:notification/notify true
             (format "Cannot save reaction: %s"
                     (get response :error))]]]}
     {:fx [[:dispatch [:server-error]]]})))

(re-frame/reg-event-fx
 :reaction/save-edit
 (fn [{{server-host       ::db/server-host
        proxy-path        ::db/proxy-path
        ?editing-reaction ::db/editing-reaction} :db} _]
   (when-let [{:keys [title
                      ruleset
                      active]
               ?reaction-id :id
               :as reaction} (some-> ?editing-reaction
                                     rfns/strip-condition-indices)]
     (if (valid? ::rs/new-reaction reaction)
       {:http-xhrio {:method          (if ?reaction-id :put :post)
                     :uri             (httpfn/serv-uri
                                       server-host
                                       "/admin/reaction"
                                       proxy-path)
                     :format          (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :params          (cond-> {:ruleset ruleset
                                               :active  active
                                               :title   title}
                                        ?reaction-id (assoc :reactionId ?reaction-id))
                     :on-success      [:reaction/save-edit-success]
                     :on-failure      [:reaction/server-error]
                     :interceptors    [httpfn/add-jwt-interceptor]}}
       {:fx [[:dispatch
              [:notification/notify true
               "Cannot save invalid reaction."]]]}))))

(defn- cancel-edit
  [db]
  (dissoc db
          ::db/editing-reaction
          ::db/editing-reaction-template-json
          ::db/editing-reaction-template-errors))

(re-frame/reg-event-fx
 :reaction/save-edit-success
 (fn [{:keys [db]} [_ {:keys [reactionId]}]]
   {:db (-> db
            cancel-edit
            (assoc ::db/reaction-focus reactionId))
    :fx [[:dispatch [:reaction/load-reactions]]]}))

;; TODO: :reaction/save-edit-fail

(re-frame/reg-event-fx
 :reaction/delete-confirm
 (fn [{:keys [db]} [_ reaction-id]]
   (let [{:keys [title]} (find-reaction db reaction-id)]
     {:fx [[:dispatch
            [:dialog/present
             {:prompt (format "Really delete reaction: %s?" title)
              :choices
              [{:label "Cancel"
                :dispatch [:dialog/cancel]}
               {:label "Delete"
                :dispatch [:reaction/delete reaction-id]}]}]]]})))

(re-frame/reg-event-fx
 :reaction/delete
 (fn [{{server-host       ::db/server-host
        proxy-path        ::db/proxy-path} :db} [_ reaction-id]]
   {:http-xhrio {:method          :delete
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/reaction"
                                   proxy-path)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          {:reactionId reaction-id}
                 :on-success      [:reaction/delete-success]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :reaction/delete-success
 (fn [_ _]
   {:fx [[:dispatch [:reaction/load-reactions]]
         [:dispatch [:reaction/back-to-list]]
         [:dispatch [:notification/notify false
                     "Reaction Deleted"]]]}))

(re-frame/reg-event-db
 :reaction/cancel-edit
 (fn [db _]
   (cancel-edit db)))

(re-frame/reg-event-fx
 :reaction/back-to-list
 (fn [_ _]
   ;; TODO: Whatever new needs to clear
   {:fx [[:dispatch [:reaction/unset-focus]]
         [:dispatch [:reaction/cancel-edit]]]}))

(re-frame/reg-event-db
 :reaction/edit-title
 (fn [db [_ title]]
   (assoc-in db [::db/editing-reaction :title] title)))

(re-frame/reg-event-db
 :reaction/edit-status
 (fn [db [_ select-result]]
   (assoc-in db [::db/editing-reaction :active]
             (case select-result
               "active" true
               "inactive" false))))

(defn- remove-element
  [v idx]
  (into (subvec v 0 idx)
        (subvec v (inc idx))))

(re-frame/reg-event-db
 :reaction/delete-identity-path
 (fn [db [_ idx]]
   (update-in db
              [::db/editing-reaction :ruleset :identityPaths]
              remove-element
              idx)))

(re-frame/reg-event-db
 :reaction/add-identity-path
 (fn [db _]
   (update-in db
              [::db/editing-reaction :ruleset :identityPaths]
              conj
              [""])))

(defn- init-type
  [new-type]
  (case (name new-type)
    "string" ""
    "json"   ""
    "number" 0
    "boolean" true
    "null" nil))

(defn- val-type
  [val]
  (cond
    (string? val) "string"
    (number? val) "number"
    (boolean? val) "boolean"
    (nil? val) "null"))

(defn- ensure-val-type
  "If the path calls for a different type, initialize value."
  [{:keys [path
           val
           ref] :as c}]
  (if ref
    c
    (let [{:keys [leaf-type]} (rpath/analyze-path path)]
      (if leaf-type
        (let [vtype (val-type val)]
          (assoc c :val (if (= leaf-type vtype)
                          val
                          (init-type leaf-type))))
        c))))

(re-frame/reg-event-db
 :reaction/add-path-segment
 (fn [db [_ path-path]]
   (let [full-path (into [::db/editing-reaction]
                         path-path)
         path-before (get-in db full-path)
         {:keys [next-keys]} (rpath/analyze-path
                              path-before)
         parent-path (butlast full-path)]
     (-> db
       (update-in full-path
                  conj
                  (if (= '[idx] next-keys) 0 ""))
       (update-in
        parent-path
        ensure-val-type)))))

(re-frame/reg-event-db
 :reaction/del-path-segment
 (fn [db [_ path-path]]
   (let [full-path (into [::db/editing-reaction]
                         path-path)
         path-before (get-in db full-path)
         path-after (vec (butlast path-before))
         parent-path (butlast full-path)]
     (-> db
         (assoc-in full-path path-after)
         (update-in
          parent-path
          ensure-val-type)))))

(re-frame/reg-event-fx
 :reaction/change-path-segment
 (fn [{:keys [db]} [_ path-path new-seg-val open-next?]]
   (let [full-path           (into [::db/editing-reaction]
                                   path-path)
         path-before         (get-in db full-path)
         path-after          (conj (vec (butlast path-before))
                                   new-seg-val)
         {:keys [complete?]} (rpath/analyze-path path-after)
         parent-path         (butlast full-path)]
     (cond-> {:db (-> db
                      (assoc-in full-path path-after)
                      (update-in
                       parent-path
                       ensure-val-type))}
       (and (not complete?) open-next?)
       (assoc :fx [[:dispatch [:reaction/add-path-segment path-path]]])))))

(re-frame/reg-event-db
 :reaction/set-op
 (fn [db [_ op-path new-op]]
   (let [full-path (into [::db/editing-reaction]
                         op-path)]
     (assoc-in db full-path new-op))))

(re-frame/reg-event-db
 :reaction/set-val-type
 (fn [db [_ val-path new-type]]
   (let [full-path (into [::db/editing-reaction]
                         val-path)]
     (assoc-in db full-path
               (init-type new-type)))))

(re-frame/reg-event-db
 :reaction/set-val
 (fn [db [_ val-path new-val]]
   (let [full-path (into [::db/editing-reaction]
                         val-path)]
     (assoc-in db full-path new-val))))

(re-frame/reg-event-db
 :reaction/set-ref-condition
 (fn [db [_ condition-path new-condition]]
   (let [full-path (into [::db/editing-reaction]
                         condition-path)]
     (assoc-in db full-path new-condition))))

(re-frame/reg-event-db
 :reaction/set-val-or-ref
 (fn [db [_ clause-path set-to]]
   (let [full-path (into [::db/editing-reaction]
                         clause-path)
         reaction (::db/editing-reaction db)
         condition-names (-> reaction
                             :ruleset
                             :conditions
                             keys
                             (->> (map name)))
         {:keys [path] :as clause} (get-in db full-path)
         {:keys [leaf-type]} (rpath/analyze-path path)]
     (case set-to
       "val"
       (assoc-in db
                 full-path
                 (-> clause
                     (dissoc :ref)
                     (assoc :val
                            (cond
                              (nil? leaf-type) "" ;; FIXME: This might not work
                              (= 'json leaf-type) "" ;; or this
                              :else (init-type leaf-type)))))
       "ref"
       (assoc-in db
                 full-path
                 (-> clause
                     (dissoc :val)
                     (assoc :ref {:condition (first condition-names)
                                  :path []})))))))

(re-frame/reg-event-db
 :reaction/set-clause-type
 (fn [db [_ clause-path clause-type]] ;; #{"and" "or" "not" "logic"}
   (let [full-path (into [::db/editing-reaction]
                         clause-path)
         {and-clauses :and
          or-clauses :or
          :as clause} (get-in db full-path)]
     (assoc-in db full-path
               (-> clause
                   ;; preserve sort idx if there
                   (select-keys [:sort-idx])
                   (merge
                    (case clause-type
                      "and" {:and (or or-clauses [])}
                      "or" {:or (or and-clauses [])}
                      "not" {:not nil}
                      "logic" {:path [""]
                               :op "eq"
                               :val ""})))))))

(re-frame/reg-event-db
 :reaction/set-condition-name
 (fn [db [_ old-name new-name]]
   (let [reaction (::db/editing-reaction db)
         all-names (-> reaction
                       :ruleset
                       :conditions
                       keys
                       set)]
     (if (contains? all-names old-name)
       (let [other-names (disj all-names old-name)]
         (if (contains? other-names new-name)
           ;; TODO: maybe pop error
           db
           (let [condition-val (-> reaction
                                   :ruleset
                                   :conditions
                                   old-name)]
             (assoc db ::db/editing-reaction
                    (-> reaction
                        (update-in [:ruleset :conditions] dissoc old-name)
                        (assoc-in [:ruleset :conditions new-name] condition-val))))))
       db))))

(re-frame/reg-event-db
 :reaction/delete-clause
 (fn [db [_ clause-path]]
   (let [full-path (into [::db/editing-reaction]
                         clause-path)
         parent-path (butlast full-path)
         k (last full-path)]
     (cond
       ;; is an element in a list
       (number? k)
       (let [parent (get-in db parent-path)
             new-parent (remove-element parent k)]
         (assoc-in db parent-path new-parent))
       ;; is at the root of a condition, remove everything but the sort
       (keyword? k)
       (update-in db full-path select-keys [:sort-idx])))))

(re-frame/reg-event-db
 :reaction/add-condition
 (fn [db [_ ?condition-key]]
   (let [k (or ?condition-key (keyword (format "condition_%s"
                                               (fns/rand-alpha-str 8))))]
     (-> db
         (update-in
          [::db/editing-reaction :ruleset :conditions]
          assoc k {:path [""]
                   :op   "eq"
                   :val  ""})
         (update
          ::db/editing-reaction
          rfns/index-conditions)))))

(re-frame/reg-event-db
 :reaction/delete-condition
 (fn [db [_ condition-key]]
   (-> db
       (update-in
        [::db/editing-reaction :ruleset :conditions]
        dissoc condition-key)
       (update
        ::db/editing-reaction
        rfns/index-conditions))))

(re-frame/reg-event-db
 :reaction/add-clause
 (fn [db [_ parent-path clause-type]] ;; :and, :or, :not, :logic
   (let [pkey (last parent-path) ;; :and, :or, :not, <condition name>
         full-path (into [::db/editing-reaction]
                         parent-path)
         new-clause (case clause-type
                      :and   {:and []}
                      :or    {:or []}
                      :not   {:not nil}
                      :logic {:path [""]
                              :op   "eq"
                              :val  ""})]
     (-> (if (contains? #{:and :or} pkey)
           (update-in db full-path conj new-clause)
           (assoc-in db full-path new-clause))
         (update
          ::db/editing-reaction
          rfns/index-conditions)))))

(re-frame/reg-event-db
 :reaction/update-template
 (fn [db [_ new-value]]
   (-> db
       (dissoc ::db/editing-reaction-template-errors)
       (assoc-in [::db/editing-reaction :ruleset :template] new-value))))

(re-frame/reg-event-db
 :reaction/set-template-errors
 (fn [db [_ errors]]
   (assoc db ::db/editing-reaction-template-errors errors)))

(re-frame/reg-event-db
 :reaction/clear-template-errors
 (fn [db _]
   (dissoc db ::db/editing-reaction-template-errors)))

(re-frame/reg-event-fx
 :reaction/set-template-json
 (fn [{:keys [db]} [_ json]]
   (let [xapi-errors (rfns/validate-template-xapi json)]
     (cond-> {:db (assoc db ::db/editing-reaction-template-json json)}
       (seq xapi-errors)
       (assoc :fx [[:dispatch [:reaction/set-template-errors xapi-errors]]])))))

(re-frame/reg-event-db
 :reaction/clear-template-json
 (fn [db _]
   (dissoc db ::db/editing-reaction-template-json)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dialog
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db
 :dialog/set-ref
 (fn [db [_ dialog-ref]]
   (assoc db ::db/dialog-ref dialog-ref)))

(re-frame/reg-event-db
 :dialog/clear-data
 (fn [db _]
   (dissoc db ::db/dialog-data)))

(re-frame/reg-fx
 :dialog/show
 (fn [{:keys [dialog-ref]}]
   (.addEventListener
    dialog-ref
    "close",
    (fn [_]
      (re-frame/dispatch [:dialog/clear-data]))
    #js {:once true})
   (.showModal dialog-ref)))

(re-frame/reg-fx
 :dialog/close
 (fn [{:keys [dialog-ref]}]
   (.close dialog-ref)))

(re-frame/reg-event-fx
 :dialog/present
 (fn [{:keys [db]} [_ dialog-data]]
   (when-let [dialog-ref (::db/dialog-ref db)]
     {:db          (assoc db ::db/dialog-data dialog-data)
      :dialog/show {:dialog-ref dialog-ref}})))

(re-frame/reg-event-fx
 :dialog/cancel
 (fn [{:keys [db]} _]
   (let [dialog-ref (::db/dialog-ref db)]
     {:dialog/close {:dialog-ref dialog-ref}})))

(re-frame/reg-event-fx
 :dialog/dispatch
 (fn [{:keys [db]} [_ dispatch-v]]
   (let [dialog-ref (::db/dialog-ref db)]
     {:dialog/close {:dialog-ref dialog-ref}
      :fx
      [[:dispatch dispatch-v]]})))
