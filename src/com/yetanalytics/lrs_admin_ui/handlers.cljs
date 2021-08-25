(ns com.yetanalytics.lrs-admin-ui.handlers
  (:require [re-frame.core  :as re-frame]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [day8.re-frame.http-fx]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.http :as httpfn]
            [com.yetanalytics.lrs-admin-ui.functions.storage :as stor]
            [ajax.core :as ajax]
            [clojure.string :as s]))

(def global-interceptors
  [db/check-spec-interceptor])

(re-frame/reg-event-db
 :db/init
 global-interceptors
 (fn [_ [_ & [?server-host
              ?xapi-prefix]]]
   {::db/session {:page :credentials
                  :token (stor/get-item "lrs-jwt")
                  :username (stor/get-item "username")}
    ::db/login {:username "username"
                :password "password"}
    ::db/credentials []
    ::db/accounts []
    ::db/new-account {:username nil
                      :password nil}
    ::db/browser {:content nil
                  :address nil
                  :credential nil}
    ::db/server-host (or ?server-host
                         "")
    ::db/xapi-prefix (or ?xapi-prefix
                         "/xapi")}))

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

(re-frame/reg-event-db
 :login/error
 global-interceptors
 (fn [db [_ error]]
   (assoc-in db [::db/login :error] error)))

(re-frame/reg-event-fx
 :session/authenticate
 global-interceptors
 (fn [{{server-host ::db/server-host} :db} _]
   {:http-xhrio {:method          :post
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/account/login")
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          @(re-frame/subscribe [:db/get-login])
                 :on-success      [:login/success-handler]
                 :on-failure      [:login/login-failure]}}))

(re-frame/reg-event-fx
 :login/success-handler
 global-interceptors
 (fn [{:keys [db]} [_ {:keys [json-web-token]}]]
   (let [username @(re-frame/subscribe [:login/get-username])]
     {:fx [[:dispatch [:session/set-token json-web-token]]
           [:dispatch [:session/set-username username]]]})))

(re-frame/reg-event-fx
 :session/set-username
 global-interceptors
 (fn [{:keys [db]} [_ username]]
   {:db (assoc-in db [::db/session :username]
                  username)
    :session/store ["username" username]}))

(re-frame/reg-fx
 :session/store
 (fn [[key value]]
   (if value
     (stor/set-item! key value)
     (stor/remove-item! key))))

(re-frame/reg-event-fx
 :session/set-token
 global-interceptors
 (fn [{:keys [db]} [_ token]]
   {:db (assoc-in db [::db/session :token] token)
    :session/store ["lrs-jwt" token]}))

(re-frame/reg-event-db
 :session/login-failure
 global-interceptors
 (fn [db [_ {:keys [body]}]]
   (assoc-in db [::db/login :error] body)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; General
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :session/set-page
 global-interceptors
 (fn [{:keys [db]} [_ page]]
   {:db (assoc-in db [::db/session :page] page)}))

(re-frame/reg-event-fx
 :server-error
 (fn [{:keys [db]} [_ {:keys [body status] :as response}]]
   (cond
     (= status 401) {:dispatch [:session/set-token nil]})))

(re-frame/reg-event-fx
 :session/logout
 (fn [_ _]
   {:fx [[:dispatch [:session/set-token nil]]
         [:dispatch [:session/set-username nil]]]}))


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
   (let [credential (first (filter  #(= key (:api-key %))
                                    @(re-frame/subscribe [:db/get-credentials])))]
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
 (fn [{{server-host ::db/server-host
        :as db} :db} [_ credential]]
   {:http-xhrio {:method          :put
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds")
                 :params          credential
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/load-credentials]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :credentials/create-credential
 global-interceptors
 (fn [{{server-host ::db/server-host
        :as db} :db} [_ credential]]
   {:http-xhrio {:method          :post
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds")
                 :params          credential
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/load-credentials]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :credentials/delete-credential
 global-interceptors
 (fn [{{server-host ::db/server-host
        :as db} :db} [_ credential]]
   {:http-xhrio {:method          :delete
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/creds")
                 :params          credential
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/load-credentials]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

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
 (fn [{{server-host ::db/server-host} :db} [_ account-id]]
   {:http-xhrio {:method          :delete
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/account")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          {:account-id account-id}
                 :format          (ajax/json-request-format)
                 :on-success      [:accounts/load-accounts]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-db
 :accounts/set-accounts
 global-interceptors
 (fn [db [_ accounts]]
   (assoc db ::db/accounts accounts)))

(re-frame/reg-event-fx
 :accounts/create-account
 global-interceptors
 (fn [{{server-host ::db/server-host} :db} _]
   {:http-xhrio {:method          :post
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/account/create")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          @(re-frame/subscribe [:db/get-new-account])
                 :format          (ajax/json-request-format)
                 :on-success      [:new-account/create-success]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-db
 :new-account/set-new-account
 global-interceptors
 (fn [db [_ new-account]]
   (assoc db ::db/new-account new-account)))

(re-frame/reg-event-fx
 :new-account/create-success
 global-interceptors
 (fn [_ _]
   {:fx [[:dispatch [:accounts/load-accounts]]
         [:dispatch [:new-account/set-new-account
                     {:username nil
                      :password nil}]]]}))

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
   {:dispatch [:new-account/set-password (fns/pass-gen 12)]}))
