(ns com.yetanalytics.lrs-admin-ui.handlers
  (:require [re-frame.core                                    :as re-frame]
            [reagent.core                                     :as r]
            [com.yetanalytics.re-route                        :as re-route]
            [com.yetanalytics.lrs-admin-ui.db                 :as db]
            [com.yetanalytics.lrs-admin-ui.input              :as input]
            [com.yetanalytics.lrs-admin-ui.routes             :refer [routes]]
            [day8.re-frame.http-fx]
            [com.yetanalytics.lrs-admin-ui.functions          :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.download :as download]
            [com.yetanalytics.lrs-admin-ui.functions.http     :as httpfn]
            [com.yetanalytics.lrs-admin-ui.functions.storage  :as stor]
            [com.yetanalytics.lrs-admin-ui.functions.password :as pass]
            [com.yetanalytics.lrs-admin-ui.functions.oidc     :as oidc]
            [com.yetanalytics.lrs-admin-ui.functions.time     :as t]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-admin-ui.functions.session  :refer [login-dispatch*
                                                                      login-dispatch]]
            [com.yetanalytics.re-oidc                         :as re-oidc]
            [ajax.core                                        :as ajax]
            [cljs.spec.alpha                                  :refer [valid?]]
            [goog.string                                      :refer [format]]
            goog.string.format
            [clojure.walk                                     :as w]
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
   {:db {::db/session {:page     :credentials
                       ;; Token and username will be set in `:db/verify-login`
                       :token    nil
                       :username nil}
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
         ::db/resource-base (if server-host "/" "/admin/")
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
    :fx [[:dispatch [:db/verify-login]]
         [:dispatch [:db/get-env]]]}))

(re-frame/reg-event-fx
 :db/get-env
 (fn [{{server-host ::db/server-host} :db} _]
   (let [?window-host (->> js/window.location.pathname
                           (re-matches #"(.+)/admin/ui.*")
                           second)]
     {:http-xhrio {:method          :get
                   ;; Check if this is prod or dev. If prod, then `server-host`
                   ;; is empty, so take the host from the browser URL.
                   :uri             (if (and (= server-host "")
                                             (some? ?window-host))
                                      (str ?window-host "/admin/env")
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
                                     enable-admin-delete-actor
                                     enable-admin-status
                                     enable-reactions
                                     no-val?
                                     no-val-logout-url
                                     admin-language-code
                                     stmt-get-max
                                     custom-language]
                       ?oidc        :oidc
                       ?oidc-enable :oidc-enable-local-admin
                       :as          env}]]
   (let [ui-route-env             (select-keys env [:proxy-path
                                                    :enable-admin-delete-actor
                                                    :enable-admin-status
                                                    :enable-reactions])
         ui-routes                (routes ui-route-env)
         jwt-refresh-interval*    (* 1000 jwt-refresh-interval)
         jwt-interaction-window*  (* 1000 jwt-interaction-window)
         oidc-enable-local-admin? (or ?oidc-enable false)
         admin-lang-keyword       (keyword admin-language-code)
         language-map             (merge-with merge
                                              lang/language
                                              custom-language)]
     ;; TODO: Put env vars in their own map
     {:db (assoc db
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
                 ::db/language language-map
                 ::db/no-val? no-val?
                 ::db/no-val-logout-url (when no-val?
                                          (not-empty no-val-logout-url)))
      :fx (cond-> [[:dispatch [::re-route/init
                               ui-routes
                               :not-found
                               {:enabled? false}]]]
            ?oidc   (conj [:dispatch [:oidc/init ?oidc]])
            no-val? (conj [:dispatch [:session/proxy-token-init]]))
      :session/store ["proxy-path" proxy-path]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login / Auth
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :db/verify-login
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} _]
   ;; Token should be empty for OIDC auth
   ;; If there are, then we get an error, which is appropriate for such an
   ;; incongrous mix of authentication procedures.
   (let [curr-token (stor/get-item "lrs-jwt")
         curr-uname (stor/get-item "username")]
     (if (some? curr-token)
       {:http-xhrio
        {:method          :get
         :uri             (httpfn/serv-uri
                           server-host
                           "/admin/verify"
                           proxy-path)
         :response-format (ajax/json-response-format {:keywords? true})
         :on-success      [:db/verify-login-success curr-token curr-uname]
         :on-failure      [:db/verify-login-error]
         :interceptors    [(httpfn/add-jwt-interceptor* curr-token)]}}
       ;; No JWT in local storage, no-op
       {}))))

(re-frame/reg-event-db
 :db/verify-login-success
 (fn [db [_ curr-token curr-uname _]]
   (-> db
       (assoc-in [::db/session :token] curr-token)
       (assoc-in [::db/session :username] curr-uname))))

(re-frame/reg-event-fx
 :db/verify-login-error
 (fn [_ [_ {:keys [status] :as error}]]
   (if (= 401 status)
     {:fx [[:dispatch [:notification/notify true
                       "Current login has expired"]]
           [:dispatch [:session/clear-token]]
           [:dispatch [:session/clear-username]]]}
     {:fx [[:dispatch [:server-error error]]]})))

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
   (let [unk-msg (format "Unknown Error, status: %s" status)
         err-msg (get response "error" unk-msg)
         message (cond
                   (= 0 status)
                   "Could not connect to LRS!"
                   (= 401 status)
                   "Unauthorized action, please log in!"
                   (and err-msg (< (count err-msg) 100))
                   (str "Error from server: " err-msg)
                   :else
                   "An unexpected error has occured!")]
     {:fx (cond-> [[:dispatch [:notification/notify true message]]]
            (some #(= status %) [0 401])
            (merge [:dispatch [:session/clear-token]]))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login / Auth
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Localstore values: JWT and Username ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-fx
 :session/store
 (fn [[key value]]
   (if value
     (stor/set-item! key value)
     (stor/remove-item! key))))

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

;; /admin/me call - gets username

(re-frame/reg-event-fx
 :session/get-me
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path
        ?oidc-auth  ::db/oidc-auth} :db} _]
   (when-not ?oidc-auth
     {:http-xhrio
      {:method          :get
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

;; Regular JWT Login ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Username + Password Buffer

(re-frame/reg-event-db
 :login/set-username
 (fn [db [_ username]]
   (assoc-in db [::db/login :username] username)))

(re-frame/reg-event-db
 :login/set-password
 (fn [db [_ password]]
   (assoc-in db [::db/login :password] password)))

;; Login

(re-frame/reg-event-fx
 :session/authenticate
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path
        :as         db} :db} _]
   {:http-xhrio
    {:method          :post
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
           ;; Have to hardcode seeding the home (i.e. the credential) page
           ;; due to race condition with set-token
           [:dispatch [::re-route/navigate :home]]
           [:dispatch [:credentials/load-credentials]]
           [:dispatch-later {:ms       jwt-refresh-interval
                             :dispatch [:login/try-renew]}]]})))

;; Renewal

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
       {:fx [[:dispatch [:logout/logout]]]}))))

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

(def ^:private renew-error-msg
  "Can no longer renew login. Congratulations on being SQL LRS's biggest fan!")

(re-frame/reg-event-fx
 :login/renew-error
 (fn [_ [_ {:keys [status] :as error}]]
   (if (= 401 status)
     {:fx [[:dispatch [:notification/notify true renew-error-msg]]
           [:dispatch [:logout/logout]]]}
     {:fx [[:dispatch [:server-error error]]]})))

;; Proxy JWT Login ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def placeholder-token
  "Sample token used for testing no-val JWT mode. This token has this body:
   ```
   {
     \"domain\": \"https://unsecure.yetanalytics.com/realm\",
     \"perms\": [\"ADMIN\"],
     \"username\": \"CLIFF.CASEY.1234567890\"
   }
   ```
   which correspond to the following values in lrsql config:
   ```
   {
     \"jwtNoVal\": true,
     \"jwtNoValUname\": \"username\",
     \"jwtNoValIssuer\": \"domain\",
     \"jwtNoValRoleKey\": \"perms\",
     \"jwtNoValRole\": \"ADMIN\"
   }
   ```
   "
  "eyJhbGciOiJIUzI1NiJ9.eyJkb21haW4iOiJodHRwczovL3Vuc2VjdXJlLnlldGFuYWx5dGljcy5jb20vcmVhbG0iLCJwZXJtcyI6WyJBRE1JTiJdLCJ1c2VybmFtZSI6IkNMSUZGLkNBU0VZLjEyMzQ1Njc4OTAifQ.2gRn_tDFBfJx2RE0pgvPM4wH__RnHf1E9kjsNlkLrnQ")

(re-frame/reg-event-fx
 :session/proxy-token-init
 (fn [_ _]
   ;; In this mode the token will be overwritten, so just store something and
   ;; move on.
   {:fx [[:dispatch [:session/set-token placeholder-token]]]}))

;; OIDC Login ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;; Logout ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :logout/logout
 (fn [{:keys [db]} _]
   (let [{server-host        ::db/server-host
          proxy-path         ::db/proxy-path
          no-val?            ::db/no-val?
          ?no-val-logout-url ::db/no-val-logout-url} db
          logged-in? (oidc/logged-in? db)]
     (cond
       ;; OIDC login
       logged-in?
       {:fx [[:dispatch [:session/clear-token]]
             [:dispatch [:session/clear-username]]
             [:dispatch [::re-oidc/logout]]]}
       ;; Proxy JWT login
       no-val?
       {:fx [[:dispatch [:session/clear-token]]
             [:dispatch [:session/clear-username]]
             (if ?no-val-logout-url
               [:logout/no-val-logout-redirect
                {:logout-url ?no-val-logout-url}]
               ;; Ideally should not happen but we need a fallback if
               ;; jwtNoValLogoutUrl is not set.
               [:dispatch [::re-route/navigate :home]])]}
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
         :on-success      [:logout/success-handler]
         :on-failure      [:server-error]
         :interceptors    [httpfn/add-jwt-interceptor]}}))))

(re-frame/reg-event-fx
 :logout/success-handler
 (fn [_ _]
   {:fx [[:dispatch [:session/clear-token]]
         [:dispatch [:session/clear-username]]
         [:dispatch [:notification/notify false "You have logged out."]]
         [:dispatch [::re-route/navigate :home]]]}))

(re-frame/reg-fx
 :logout/no-val-logout-redirect
 (fn [{:keys [logout-url]}]
   (set! (.-location js/window) logout-url)))

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

(defmethod re-route/on-start :browser [{:keys [db]} _params]
  (login-dispatch* db))

(re-frame/reg-event-fx
 :browser/try-load-xapi
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path
        ?oidc-auth  ::db/oidc-auth} :db} [_ opts]]
   (if-not ?oidc-auth
     {:http-xhrio
      {:method          :get
       :uri             (httpfn/serv-uri
                         server-host
                         "/admin/verify"
                         proxy-path)
       :response-format (ajax/json-response-format {:keywords? true})
       :on-success      [:browser/load-xapi opts]
       :on-failure      [:server-error]
       :interceptors    [httpfn/add-jwt-interceptor]}}
     {:fx [[:dispatch [:browser/load-xapi opts]]]})))

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
      :dispatch [:browser/try-load-xapi {:params more-params}]})))

(re-frame/reg-event-fx
 :browser/back
 (fn [{:keys [db]} _]
   ;; Pop most recent from stack
   (let [back-stack (get-in db [::db/browser :back-stack])
         back-params (httpfn/extract-params (peek back-stack))]
     {:db (update-in db [::db/browser :back-stack] pop)
      :dispatch [:browser/try-load-xapi {:params back-params}]})))

(re-frame/reg-event-fx
 :browser/add-filter
 (fn [{:keys [db]} [_ param-key param-value]]
   (let [address (get-in db [::db/browser :address])
         params (-> (httpfn/extract-params address)
                    (dissoc "from" "limit")
                    (assoc param-key param-value))]
     ;; Clear back-stack
     {:db (assoc-in db [::db/browser :back-stack] [])
      :dispatch [:browser/try-load-xapi {:params params}]})))

(re-frame/reg-event-fx
 :browser/clear-filters
 (fn [{:keys [db]} _]
   {;; Clear back-stack and reset query
    :db (assoc-in db [::db/browser :back-stack] [])
    :dispatch [:browser/try-load-xapi]}))

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
        :dispatch [:browser/try-load-xapi]}))))

(re-frame/reg-event-fx
 :browser/refresh
 (fn [{:keys [db]} _]
   (when (get-in db [::db/browser :credential])
     ;; Clear backstack
     {:db (assoc-in db [::db/browser :back-stack] [])
      :dispatch [:browser/try-load-xapi]})))

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
      :dispatch [:browser/try-load-xapi {:params params}]})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Api Key Management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- has-seed-cred?
  [credentials]
  (boolean (some (fn [cred] (when (:seed? cred) cred)) credentials)))

(re-frame/reg-event-fx
 :credentials/notify-on-seed
 (fn [{:keys [db]} [_ credentials]]
   (let [credentials (or (not-empty credentials)
                         (::db/credentials db))]
     (if (has-seed-cred? credentials)
       {:fx [[:dispatch [:notification/notify true "Seed credentials should be deleted!"]]]}
       {}))))

(defmethod re-route/on-start :not-found [{:keys [db]} _params]
  (login-dispatch* db))

(defmethod re-route/on-start :home [{:keys [db]} _params]
  (login-dispatch db [:credentials/load-credentials]))

(defmethod re-route/on-start :credentials [{:keys [db]} _params]
  (login-dispatch db [:credentials/load-credentials]))

(re-frame/reg-event-fx
 :credentials/set-credentials
 (fn [db [_ credentials]]
   {:db (assoc db ::db/credentials credentials)
    :fx [[:dispatch [:credentials/notify-on-seed credentials]]]}))

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

(defmethod re-route/on-start :accounts [{:keys [db]} _params]
  (login-dispatch db [:accounts/load-accounts]))

(defmethod re-route/on-start :update-password [{:keys [db]} _params]
  (login-dispatch* db))

(defmethod re-route/on-stop :update-password [_ _]
  {:fx [[:dispatch [:update-password/clear]]]})

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
         [:dispatch [::re-route/navigate :credentials]]
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

(defmethod re-route/on-start :data-management [{:keys [db]} _params]
  (login-dispatch* db))

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
         [:dispatch [:browser/try-load-xapi]]]}))

(re-frame/reg-event-fx
 :delete-actor/server-error
 (fn [_ [_ actor-ifi _err]]
   {:fx [[:dispatch  [:notification/notify true  (str "Error when attempting to delete actor " actor-ifi)]]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Status Dashboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod re-route/on-start :status [{:keys [db]} _params]
  (login-dispatch db [:status/get-all-data]))

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

(re-frame/reg-event-fx
 :status/get-all-data
 (fn [_]
   {:fx [[:dispatch [:status/get-data ["statement-count"]]]
         [:dispatch [:status/get-data ["actor-count"]]]
         [:dispatch [:status/get-data ["last-statement-stored"]]]
         [:dispatch [:status/get-data ["timeline"]]]
         [:dispatch [:status/get-data ["platform-frequency"]]]]}))

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

(defmethod re-route/on-start :reactions [{:keys [db]} _]
  (login-dispatch db [:reaction/load-reactions]))

(defmethod re-route/on-start :reactions/new [{:keys [db]} _]
  (login-dispatch db [:reaction/set-new]))

(defmethod re-route/on-start :reactions/focus [{:keys [db]} [_ _ reaction-id]]
  (login-dispatch db [:reaction/load-reactions-and-focus reaction-id]))

(defmethod re-route/on-start :reactions/edit [{:keys [db]} [_ _ reaction-id]]
  (login-dispatch db [:reaction/load-reactions-and-edit reaction-id]))

(defmethod re-route/on-stop :reactions/new [_ _]
  {:fx [[:dispatch [:reaction/clear-edit]]]})

(defmethod re-route/on-stop :reactions/edit [_ _]
  {:fx [[:dispatch [:reaction/clear-edit]]]})

(defmethod re-route/on-stop :reactions/focus [_ _]
  {:fx [[:dispatch [:reaction/unset-focus]]]})

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
          (mapv rfns/db->focus-form reactions))))

;; Reaction View ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :reaction/load-reactions-and-focus
 (fn [{:keys [db]} [_ reaction-id]]
   (if (empty? (get db ::db/reactions))
     {:fx [[:dispatch [:reaction/load-reactions]]
           [:dispatch [:reaction/set-focus reaction-id]]]}
     {:fx [[:dispatch [:reaction/set-focus reaction-id]]]})))

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
   (let [reactions (rfns/focus->download-form (::db/reactions db))]
     {:download-edn [reactions "reactions"]})))

(re-frame/reg-event-fx
 :reaction/download
 (fn [{:keys [db]} [_ reaction-id]]
   (if-let [reaction (some-> (find-reaction db reaction-id)
                             rfns/focus->download-form)]
     {:download-edn [reaction (:title reaction)]}
     {:fx [[:dispatch [:notification/notify true
                       "Cannot download, reaction not found!"]]]})))

(re-frame/reg-event-fx
 :reaction/upload
 (fn [{:keys [db]}]
   {:db db}))

;; Reaction Edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: Somehow refactor this effect, since right now it's mostly copy-pasted
;; code from :reaction/load-reactions.

(re-frame/reg-event-fx
 :reaction/load-reactions-and-edit
 (fn [{{reactions   ::db/reactions
        server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} [_ reaction-id]]
   (if (empty? reactions)
     {:http-xhrio {:method          :get
                   :uri             (httpfn/serv-uri
                                     server-host
                                     "/admin/reaction"
                                     proxy-path)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:reaction/set-reactions-and-edit reaction-id]
                   :on-failure      [:server-error]
                   :interceptors    [httpfn/add-jwt-interceptor]}}
     {:fx [[:dispatch [:reaction/set-edit reaction-id]]]})))

(defn- set-reaction-edit [db reaction-id]
  (if-let [reaction (some-> (find-reaction db reaction-id)
                            rfns/focus->edit-form)]
    {:db (-> db
             (assoc ::db/editing-reaction reaction)
             prep-edit-reaction-template)}
    {:fx [[:dispatch [:notification/notify true
                      "Cannot edit, reaction not found!"]]]}))

(re-frame/reg-event-fx
 :reaction/set-reactions-and-edit
 (fn [{:keys [db]} [_ reaction-id {:keys [reactions]}]]
   (let [db* (assoc db
                    ::db/reactions
                    (mapv rfns/db->focus-form reactions))]
     (set-reaction-edit db* reaction-id))))

(re-frame/reg-event-fx
 :reaction/set-edit
 (fn [{:keys [db]} [_ reaction-id]]
   (set-reaction-edit db reaction-id)))

(re-frame/reg-event-fx
 :reaction/set-new
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
                    :conditions    []
                    :template      {"actor"
                                    {"name" "Actor Example",
                                     "mbox" "mailto:actor_example@yetanalytics.com"},
                                    "object" {"id" "https://www.yetanalytics.com/xapi/activities/example_activity"},
                                    "verb" {"id" "https://adlnet.gov/expapi/verbs/completed"}}}}]
     {:db (-> db
              (assoc ::db/editing-reaction reaction)
              prep-edit-reaction-template)})))

(re-frame/reg-event-fx
 :reaction/upload-edit
 (fn [{:keys [db]} [_ upload-data]]
   (if-some [edn-data (try
                          (js->clj (js/JSON.parse upload-data)
                                   :keywordize-keys true)
                          (catch js/Error _ nil))]
     (let [reaction (-> edn-data
                        rfns/upload->edit-form)]
       (if (valid? ::rse/reaction reaction)
         {:db (-> db
                  (assoc ::db/editing-reaction reaction)
                  prep-edit-reaction-template)}
         {:fx [[:dispatch [:notification/notify true
                           "Cannot upload invalid reaction"]]]}))
     {:fx [[:dispatch [:notification/notify true
                       "Cannot upload invalid JSON data as reaction"]]]})))

(re-frame/reg-event-fx
 :reaction/revert-edit
 (fn [{:keys [db]} _]
   (when-let [reaction-id (get-in db [::db/editing-reaction :id])]
     (when-let [reaction (some-> (find-reaction db reaction-id)
                                 rfns/focus->edit-form)]
       {:db (-> db
                (assoc ::db/editing-reaction reaction)
                prep-edit-reaction-template)}))))

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
   (when-let [{?reaction-id :id
               :as reaction} ?editing-reaction]
     (if (valid? :validation/reaction reaction)
       {:http-xhrio {:method          (if ?reaction-id :put :post)
                     :uri             (httpfn/serv-uri
                                       server-host
                                       "/admin/reaction"
                                       proxy-path)
                     :format          (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :params          (rfns/edit->db-form reaction)
                     :on-success      [:reaction/save-edit-success]
                     :on-failure      [:reaction/server-error]
                     :interceptors    [httpfn/add-jwt-interceptor]}}
       {:fx [[:dispatch
              [:notification/notify true
               "Cannot save invalid reaction."]]]}))))

(re-frame/reg-event-fx
 :reaction/save-edit-success
 (fn [_ [_ {:keys [reactionId]}]]
   ;; TODO: Only reload the reaction being updated, rather than all of them.
   ;; TODO: Display some sort of loading screen before reaction is updated
   ;; (for slow connection).
   {:fx [[:dispatch [:reaction/load-reactions]]
         [:dispatch [::re-route/navigate :reactions/focus {:id reactionId}]]]}))

(re-frame/reg-event-db
 :reaction/clear-edit
 (fn [db _]
   (dissoc db
           ::db/editing-reaction
           ::db/editing-reaction-template-json
           ::db/editing-reaction-template-errors)))

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
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} [_ reaction-id]]
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
         [:dispatch [:notification/notify false
                     "Reaction Deleted"]]]}))

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
          (assoc c :val (if (= (str leaf-type) vtype)
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
         {:keys [leaf-type
                 complete?]} (rpath/analyze-path path-after)
         parent-path         (butlast full-path)]
     (cond-> {:db (-> db
                      (assoc-in full-path path-after)
                      (update-in
                       parent-path
                       ensure-val-type))}
       (and (not complete?)
            (not= 'json leaf-type) ; not extension
            open-next?)
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
                             (->> (map :name)))
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
         {?cond-name  :name
          and-clauses :and
          or-clauses  :or
          :as         _clause} (get-in db full-path)
         new-clause
         (cond-> (case clause-type
                   "and"   {:and (or or-clauses [])}
                   "or"    {:or (or and-clauses [])}
                   "not"   {:not nil}
                   "logic" {:path [""]
                            :op   "eq"
                            :val  ""})
           ?cond-name
           (assoc :name ?cond-name))]
     (assoc-in db full-path new-clause))))

(re-frame/reg-event-db
 :reaction/set-condition-name
 (fn [db [_ cond-path new-name]]
   (let [full-path (into [::db/editing-reaction]
                         cond-path)]
     (assoc-in db full-path new-name))))

(re-frame/reg-event-db
 :reaction/delete-clause
 (fn [db [_ clause-path]]
   (let [full-path (into [::db/editing-reaction]
                         clause-path)
         parent-path (butlast full-path)
         k (last full-path)]
     (cond
       (#{:and :or} (last parent-path))
       (let [parent (get-in db parent-path)
             new-parent (remove-element parent k)]
         (assoc-in db parent-path new-parent))
       (#{:not} (last full-path))
       (assoc-in db full-path nil)
       :else
       (update-in db full-path select-keys [:name])))))

(re-frame/reg-event-db
 :reaction/add-condition
 (fn [db [_ ?condition-key]]
   (let [cond-name (or ?condition-key
                       (format "condition_%s"
                               (fns/rand-alpha-str 8)))]
     (update-in
      db
      [::db/editing-reaction :ruleset :conditions]
      conj
      {:name cond-name
       :path [""]
       :op   "eq"
       :val  ""}))))

(re-frame/reg-event-db
 :reaction/delete-condition
 (fn [db [_ condition-idx]]
   (update-in
    db
    [::db/editing-reaction :ruleset :conditions]
    remove-element
    condition-idx)))

(re-frame/reg-event-db
 :reaction/add-clause
 (fn [db [_ parent-path clause-type]] ;; :and, :or, :not, :logic
   (let [pkey (last parent-path) ;; :and, :or, :not, <condition name>
         full-path (into [::db/editing-reaction]
                         parent-path)
         ?cond-name (get-in db (conj full-path :name))
         new-clause (cond-> (case clause-type
                              :and   {:and []}
                              :or    {:or []}
                              :not   {:not nil}
                              :logic {:path [""]
                                      :op   "eq"
                                      :val  ""})
                      ?cond-name
                      (assoc :name ?cond-name))]
     (if (contains? #{:and :or} pkey)
       (update-in db full-path conj new-clause)
       (assoc-in db full-path new-clause)))))

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
