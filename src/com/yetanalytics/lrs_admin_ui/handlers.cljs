(ns com.yetanalytics.lrs-admin-ui.handlers
  (:require [re-frame.core   :as re-frame]
            [reagent.core    :as r]
            [ajax.core       :as ajax]
            [cljs.spec.alpha :refer [valid?]]
            [clojure.string  :refer [split]]
            [goog.string     :refer [format]]
            [goog.string.format]
            [day8.re-frame.http-fx]
            [com.yetanalytics.re-oidc                         :as re-oidc]
            [com.yetanalytics.lrs-admin-ui.db                 :as db]
            [com.yetanalytics.lrs-admin-ui.input              :as input]
            [com.yetanalytics.lrs-admin-ui.functions          :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.download :as download]
            [com.yetanalytics.lrs-admin-ui.functions.http     :as httpfn]
            [com.yetanalytics.lrs-admin-ui.functions.storage  :as stor]
            [com.yetanalytics.lrs-admin-ui.functions.password :as pass]
            [com.yetanalytics.lrs-admin-ui.functions.oidc     :as oidc]
            [com.yetanalytics.lrs-admin-ui.language           :as lang]
            [com.yetanalytics.lrs-admin-ui.handlers.util      :refer [global-interceptors
                                                                      page-fx]]
            [com.yetanalytics.lrs-admin-ui.handlers.browser]
            [com.yetanalytics.lrs-admin-ui.handlers.reaction]
            [com.yetanalytics.lrs-admin-ui.handlers.status]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :db/init
 global-interceptors
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
         ::db/stmt-get-max 10
         ::db/enable-admin-delete-actor false
         ::db/notifications []
         ::db/oidc-auth false
         ::db/oidc-enable-local-admin false
         ::db/enable-admin-status false
         ::db/status {}
         ::db/enable-reactions false
         ::db/reactions []}
    :fx [[:dispatch [:db/get-env]]]}))

(re-frame/reg-event-fx
 :db/get-env
 global-interceptors
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
 global-interceptors
 (fn [{:keys [db]} [_ {:keys             [url-prefix
                                          proxy-path
                                          enable-admin-status
                                          enable-reactions
                                          no-val?
                                          no-val-logout-url
                                          enable-admin-delete-actor
                                          admin-language-code
                                          stmt-get-max
                                          custom-language]
                       ?oidc             :oidc
                       ?oidc-local-admin :oidc-enable-local-admin}]]
   {:db (cond-> (assoc db
                       ::db/xapi-prefix url-prefix
                       ::db/proxy-path proxy-path
                       ::db/oidc-enable-local-admin (or ?oidc-local-admin false)
                       ::db/enable-admin-status enable-admin-status
                       ::db/enable-reactions enable-reactions
                       ::db/enable-admin-delete-actor enable-admin-delete-actor
                       ::db/stmt-get-max stmt-get-max
                       ::db/pref-lang (keyword admin-language-code)
                       ::db/language (merge-with merge
                                                 lang/language
                                                 custom-language))
          (and no-val?
               (not-empty no-val-logout-url))
          (assoc ::db/no-val-logout-url no-val-logout-url))
    :fx (cond-> []
          ?oidc (conj [:dispatch [:oidc/init ?oidc]])
          no-val? (conj [:dispatch [:session/proxy-token-init]]))
    :session/store ["proxy-path" proxy-path]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Error Handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
;; Login / Auth
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :session/proxy-token-init
 global-interceptors
 (fn [_ _]
   ;; In this mode the token will be overwritten, so just store something and
   ;; move on. For testing the feature, this placeholder token has "username",
   ;; "perms" array containing "ADMIN" perm, and "domain" as the issuer
   (let [placeholder-token "eyJhbGciOiJIUzI1NiJ9.eyJkb21haW4iOiJodHRwczovL3Vuc2VjdXJlLnlldGFuYWx5dGljcy5jb20vcmVhbG0iLCJwZXJtcyI6WyJBRE1JTiJdLCJ1c2VybmFtZSI6IkNMSUZGLkNBU0VZLjEyMzQ1Njc4OTAifQ.2gRn_tDFBfJx2RE0pgvPM4wH__RnHf1E9kjsNlkLrnQ"]
     {:fx [[:dispatch [:session/set-token placeholder-token]]]})))

(re-frame/reg-event-db
 :login/set-username
 global-interceptors
 (fn [db [_ username]]
   (assoc-in db [::db/login :username] username)))

(re-frame/reg-event-db
 :login/set-password
 global-interceptors
 (fn [db [_ password]]
   (assoc-in db [::db/login :password] password)))

(re-frame/reg-event-fx
 :session/authenticate
 global-interceptors
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
 global-interceptors
 (fn [_ [_ {:keys [json-web-token]}]]
   {:fx [[:dispatch [:session/set-token json-web-token]]
         [:dispatch [:login/set-password nil]]
         [:dispatch [:login/set-username nil]]]}))

(re-frame/reg-event-fx
 :session/set-token
 global-interceptors
 (fn [{:keys [db]} [_ token
                    & {:keys [store?]
                       :or {store? true}}]]
   (cond-> {:db (assoc-in db [::db/session :token] token)
            :fx [[:dispatch [:session/get-me]]]}
     store? (assoc :session/store ["lrs-jwt" token]))))

(re-frame/reg-event-fx
 :session/get-me
 global-interceptors
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
 global-interceptors
 (fn [_ [_ {:keys [username]}]]
   {:fx [[:dispatch [:session/set-username username]]]}))

(re-frame/reg-event-fx
 :login/error-handler
 global-interceptors
 (fn [_ [_ {:keys [status] :as error}]]
   ;; For auth, if its badly formed or not authorized give a specific error,
   ;; otherwise default to typical server error notice handling
   (if (or (= status 401) (= status 400))
     {:fx [[:dispatch [:notification/notify true
                       "Please enter a valid username and password!"]]]}
     {:fx [[:dispatch [:server-error error]]]})))

(re-frame/reg-event-fx
 :session/set-username
 global-interceptors
 (fn [{:keys [db]} [_ username
                    & {:keys [store?]
                       :or {store? true}}]]
   (cond-> {:db (assoc-in db [::db/session :username]
                          username)}
     store? (assoc :session/store ["username" username]))))

(re-frame/reg-fx
 :session/store
 (fn [[key value]]
   (if value
     (stor/set-item! key value)
     (stor/remove-item! key))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Logout
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :session/logout
 (fn [{:keys [db]} _]
   (let [{?no-val-logout-url ::db/no-val-logout-url} db]
     {:fx [[:dispatch [:session/set-token nil]]
           [:dispatch [:session/set-username nil]]
           ;; For OIDC logouts, which contain a redirect, notification is
           ;; triggered by logout success
           (cond
             (oidc/logged-in? db) [:dispatch [::re-oidc/logout]]
             ?no-val-logout-url [:session/no-val-logout-redirect
                                 {:logout-url ?no-val-logout-url}]
             :else
             [:dispatch [:notification/notify false "You have logged out."]])]})))

(re-frame/reg-fx
 :session/no-val-logout-redirect
 (fn [{:keys [logout-url]}]
   (set! (.-location js/window) logout-url)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page Set
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :session/set-page
 global-interceptors
 (fn [{:keys [db]} [_ page :as qvec]]
   {:db (assoc-in db [::db/session :page] page)
    :fx (page-fx qvec)}))

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
;; Api Key Management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: This is a separate page; consider a separate handler namespace?

(re-frame/reg-event-db
 :credentials/set-credentials
 global-interceptors
 (fn [db [_ credentials]]
   (assoc db ::db/credentials credentials)))

(re-frame/reg-event-fx
 :credentials/load-credentials
 global-interceptors
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
 global-interceptors
 (fn [db [_ idx credential]]
   (assoc-in db [::db/credentials idx] credential)))

(re-frame/reg-event-fx
 :credentials/save-credential
 global-interceptors
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
 global-interceptors
 (fn [_ [_ {:keys [api-key]}]]
   {:fx [[:dispatch [:credentials/load-credentials]]
         [:dispatch [:notification/notify false
                     (format "Updated credential with key: %s"
                             (fns/elide api-key 10))]]]}))

(re-frame/reg-event-fx
 :credentials/create-credential
 global-interceptors
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
 global-interceptors
 (fn [_ [_ {:keys [api-key]}]]
   {:fx [[:dispatch [:credentials/load-credentials]]
         [:dispatch [:notification/notify false
                     (format "Created credential with key: %s"
                             (fns/elide api-key 10))]]]}))

(re-frame/reg-event-fx
 :credentials/delete-credential
 global-interceptors
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
 global-interceptors
 (fn [_ [_ {:keys [api-key]}]]
   {:fx [[:dispatch [:credentials/load-credentials]]
         [:dispatch [:notification/notify false
                     (format "Deleted credential with key: %s"
                             (fns/elide api-key 10))]]]}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Account Management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: This is a separate page; consider a separate handler namespace?

(re-frame/reg-event-fx
 :accounts/load-accounts
 global-interceptors
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
 global-interceptors
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
 global-interceptors
 (fn [_ [_ username _]]
   {:fx [[:dispatch [:accounts/load-accounts]]
         [:dispatch [:notification/notify false
                     (format "Deleted account with username: %s" username)]]]}))

(re-frame/reg-event-db
 :accounts/set-accounts
 global-interceptors
 (fn [db [_ accounts]]
   (assoc db ::db/accounts accounts)))

(re-frame/reg-event-fx
 :accounts/create-account
 global-interceptors
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
 global-interceptors
 (fn [db [_ new-account]]
   (assoc db ::db/new-account new-account)))

(re-frame/reg-event-fx
 :accounts/create-success
 global-interceptors
 (fn [_ [_ username _]]
   {:fx [[:dispatch [:accounts/load-accounts]]
         [:dispatch [:new-account/set-new-account
                     {:username nil
                      :password nil}]]
         [:dispatch [:notification/notify false
                     (format "Created account with username: %s" username)]]]}))

(re-frame/reg-event-fx
 :accounts/create-error
 global-interceptors
 (fn [_ [_ {:keys [status] :as error}]]
   ;; For account creation, if its malformed give a specific error,
   ;; otherwise default to typical server error notice handling
   (if (= status 400)
     {:fx [[:dispatch [:notification/notify true
                       "Please enter a valid username and password!"]]]}
     {:fx [[:dispatch [:server-error error]]]})))

(re-frame/reg-event-db
 :new-account/set-username
 global-interceptors
 (fn [db [_ username]]
   (assoc-in db [::db/new-account :username] username)))

(re-frame/reg-event-db
 :new-account/set-password
 global-interceptors
 (fn [db [_ password]]
   (assoc-in db [::db/new-account :password] password)))

(re-frame/reg-event-fx
 :new-account/generate-password
 global-interceptors
 (fn [_ _]
   {:dispatch [:new-account/set-password (pass/pass-gen 12)]}))

(re-frame/reg-event-db
 :update-password/set-old-password
 global-interceptors
 (fn [db [_ password]]
   (assoc-in db [::db/update-password :old-password] password)))

(re-frame/reg-event-db
 :update-password/set-new-password
 global-interceptors
 (fn [db [_ password]]
   (assoc-in db [::db/update-password :new-password] password)))

(re-frame/reg-event-db
 :update-password/clear
 global-interceptors
 (fn [db _]
   (assoc db ::db/update-password {:old-password nil
                                   :new-password nil})))

(re-frame/reg-event-fx
 :update-password/generate-password
 global-interceptors
 (fn [_ _]
   {:dispatch [:update-password/set-new-password (pass/pass-gen 12)]}))

(re-frame/reg-event-fx
 :update-password/update-password!
 global-interceptors
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
 global-interceptors
 (fn [_ _]
   {:fx [[:dispatch [:update-password/clear]]
         [:dispatch [:session/set-page :credentials]]
         [:dispatch [:notification/notify false
                     "Password updated."]]]}))

(re-frame/reg-event-fx
 :update-password/update-error
 global-interceptors
 (fn [_ _]
   {:fx [[:dispatch [:notification/notify true
                     "Password update failed. Please try again."]]]}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete Actor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: This is a separate page; consider a separate handler namespace?

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
 global-interceptors
 (fn [_ _]
   {:fx [[:oidc/clear-search-fx {}]]}))

(re-frame/reg-event-fx
 :oidc/user-loaded
 global-interceptors
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
 global-interceptors
 (fn [_ _]
   {:fx [[:dispatch [:session/set-token nil]]
         [:dispatch [:session/set-username nil]]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dialog
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db
 :dialog/set-ref
 global-interceptors
 (fn [db [_ dialog-ref]]
   (assoc db ::db/dialog-ref dialog-ref)))

(re-frame/reg-event-db
 :dialog/clear-data
 global-interceptors
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
 global-interceptors
 (fn [{:keys [db]} [_ dialog-data]]
   (when-let [dialog-ref (::db/dialog-ref db)]
     {:db          (assoc db ::db/dialog-data dialog-data)
      :dialog/show {:dialog-ref dialog-ref}})))

(re-frame/reg-event-fx
 :dialog/cancel
 global-interceptors
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
