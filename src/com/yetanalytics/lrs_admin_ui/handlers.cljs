(ns com.yetanalytics.lrs-admin-ui.handlers
  (:require [re-frame.core                                    :as re-frame]
            [reagent.core                                     :as r]
            [com.yetanalytics.lrs-admin-ui.db                 :as db]
            [com.yetanalytics.lrs-admin-ui.input              :as input]
            [day8.re-frame.http-fx]
            [com.yetanalytics.lrs-admin-ui.functions          :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.http     :as httpfn]
            [com.yetanalytics.lrs-admin-ui.functions.storage  :as stor]
            [com.yetanalytics.lrs-admin-ui.functions.password :as pass]
            [com.yetanalytics.lrs-admin-ui.functions.oidc     :as oidc]
            [com.yetanalytics.re-oidc                         :as re-oidc]
            [ajax.core                                        :as ajax]
            [cljs.spec.alpha                                  :refer [valid?]]
            [goog.string                                      :refer [format]]
            goog.string.format))

(def global-interceptors
  [db/check-spec-interceptor])

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
         ::db/browser {:content nil
                       :address nil
                       :credential nil}
         ::db/server-host (or server-host "")
         ::db/xapi-prefix "/xapi"
         ::db/enable-statement-html true
         ::db/notifications []
         ::db/oidc-auth false
         ::db/oidc-enable-local-admin false
         ::db/enable-admin-status false
         ::db/status {}}
    :fx [[:dispatch [:db/get-env]]]}))

(re-frame/reg-event-fx
 :db/get-env
 global-interceptors
 (fn [{{server-host ::db/server-host} :db} _]
   {:http-xhrio {:method          :get
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/env")
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:db/set-env]
                 :on-failure      [:server-error]}}))

(re-frame/reg-event-fx
 :db/set-env
 global-interceptors
 (fn [{:keys [db]} [_ {:keys             [url-prefix
                                          enable-stmt-html
                                          enable-admin-status]
                       ?oidc             :oidc
                       ?oidc-local-admin :oidc-enable-local-admin}]]
   (merge {:db (assoc db
                      ::db/xapi-prefix url-prefix
                      ::db/enable-statement-html enable-stmt-html
                      ::db/oidc-enable-local-admin (or ?oidc-local-admin false)
                      ::db/enable-admin-status enable-admin-status)}
          (when ?oidc
            {:dispatch [:oidc/init ?oidc]}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login / Auth
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
        :as db} :db} _]
   {:http-xhrio {:method          :post
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/account/login")
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          (::db/login db)
                 :on-success      [:login/success-handler]
                 :on-failure      [:login/error-handler]}}))

(re-frame/reg-event-fx
 :login/success-handler
 global-interceptors
 (fn [{:keys [db]} [_ {:keys [json-web-token]}]]
   (let [{:keys [username]} (::db/login db)]
     {:fx [[:dispatch [:session/set-token json-web-token]]
           [:dispatch [:session/set-username username]]
           [:dispatch [:login/set-password nil]]
           [:dispatch [:login/set-username nil]]]})))

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

(re-frame/reg-event-fx
 :session/set-token
 global-interceptors
 (fn [{:keys [db]} [_ token
                    & {:keys [store?]
                       :or {store? true}}]]
   (cond-> {:db (assoc-in db [::db/session :token] token)}
     store? (assoc :session/store ["lrs-jwt" token]))))

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
 global-interceptors
 (fn [{:keys [db]} [_ page :as qvec]]
   {:db (assoc-in db [::db/session :page] page)
    :fx (page-fx qvec)}))

(re-frame/reg-event-fx
 :server-error
 (fn [_ [_ {:keys [response status]}]]
   ;;extract the error and present it in a notification. If 401 or 0, log out.
   (let [message (if (= status 0)
                   "Could not connect to LRS!"
                   (or (:error response)
                       "An unexpected error has occured!"))]
     {:fx (cond-> [[:dispatch [:notification/notify true message]]]
            (some #(= status %) [0 401])
            (merge [:dispatch [:session/set-token nil]]))})))

(re-frame/reg-event-fx
 :session/logout
 (fn [{:keys [db]} _]
   {:fx [[:dispatch [:session/set-token nil]]
         [:dispatch [:session/set-username nil]]
         [:dispatch
          ;; For OIDC logouts, which contain a redirect, notification is
          ;; triggered by logout success
          (if (oidc/logged-in? db)
            [::re-oidc/logout]
            [:notification/notify false "You have logged out."])]]}))

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
;; Data Browser
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :browser/load-xapi
 global-interceptors
 (fn [{{server-host ::db/server-host
        xapi-prefix ::db/xapi-prefix} :db} [_ {:keys [path params]}]]
   (let [xapi-url (httpfn/build-xapi-url
                   server-host xapi-prefix path params)]
     {:dispatch   [:browser/set-address xapi-url]
      :http-xhrio {:method          :get
                   :uri             xapi-url
                   :response-format (ajax/text-response-format)
                   :on-success      [:browser/load-stmts-success]
                   :on-failure      [:server-error]
                   :interceptors    [httpfn/format-html-interceptor]}})))

(re-frame/reg-event-db
 :browser/set-address
 (fn [db [_ address]]
   (assoc-in db [::db/browser :address] address)))

(re-frame/reg-event-db
 :browser/load-stmts-success
 global-interceptors
 (fn [db [_ response]]
   (assoc-in db [::db/browser :content] response)))

(re-frame/reg-event-fx
 :browser/update-credential
 global-interceptors
 (fn [{:keys [db]} [_ key]]
   (let [credential (first (filter #(= key (:api-key %))
                                   (::db/credentials db)))]
     (when credential
       {:db (assoc-in db [::db/browser :credential] credential)
        :dispatch [:browser/load-xapi]}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Api Key Management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db
 :credentials/set-credentials
 global-interceptors
 (fn [db [_ credentials]]
   (assoc db ::db/credentials credentials)))

(re-frame/reg-event-fx
 :credentials/load-credentials
 global-interceptors
 (fn [{{server-host ::db/server-host} :db} _]
   {:http-xhrio {:method          :get
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds")
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
 (fn [{{server-host ::db/server-host} :db} [_ credential]]
   {:http-xhrio {:method          :put
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds")
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
 (fn [{{server-host ::db/server-host} :db} [_ credential]]
   {:http-xhrio {:method          :post
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds")
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
 (fn [{{server-host ::db/server-host} :db} [_ credential]]
   {:http-xhrio {:method          :delete
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds")
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


(re-frame/reg-event-fx
 :accounts/load-accounts
 global-interceptors
 (fn [{{server-host ::db/server-host} :db} _]
   {:http-xhrio {:method          :get
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/account")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:accounts/set-accounts]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :accounts/delete-account
 global-interceptors
 (fn [{{server-host ::db/server-host} :db} [_ {:keys [account-id username]}]]
   {:http-xhrio {:method          :delete
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/account")
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
        :as db} :db} _]
   (let [{:keys [username] :as new-account}
         (::db/new-account db)]
     (if (valid? ::input/valid-new-account new-account)
       {:http-xhrio {:method          :post
                     :uri             (httpfn/serv-uri
                                       server-host
                                       "/admin/account/create")
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
;; Status Dashboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :status/get-data
 global-interceptors
 (fn [{{server-host      ::db/server-host
        {:keys [params]} ::db/status} :db} _]
   {:http-xhrio (cond-> {:method          :get
                         :uri             (httpfn/serv-uri
                                           server-host
                                           "/admin/status")
                         :format          (ajax/json-request-format)
                         :response-format (ajax/json-response-format {:keywords? true})
                         :on-success      [:status/set-data]
                         :on-failure      [:server-error]
                         :interceptors    [httpfn/add-jwt-interceptor]}
                  (not-empty params) (assoc :params (reduce-kv
                                                     (fn [m k v]
                                                       (assoc m (name k) v))
                                                     {}
                                                     params)))}))

(defmethod page-fx :status [_] [[:dispatch [:status/get-data]]])

(re-frame/reg-event-fx
 :status/set-data
 global-interceptors
 (fn [{:keys [db]} [_ status-data]]
   {:db (assoc-in db [::db/status :data] status-data)}))

(re-frame/reg-event-fx
 :status/set-timeline-unit
 global-interceptors
 (fn [{:keys [db]} [_ unit]]
   {:db (assoc-in db [::db/status :params :timeline-unit] unit)
    :fx [[:dispatch
          [:status/get-data]]]}))
