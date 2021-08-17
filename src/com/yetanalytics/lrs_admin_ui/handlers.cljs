(ns com.yetanalytics.lrs-admin-ui.handlers
  (:require [re-frame.core  :as re-frame]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [day8.re-frame.http-fx]
            [com.yetanalytics.lrs-admin-ui.functions.http :as httpfn]
            [com.yetanalytics.lrs-admin-ui.functions.storage :as stor]
            [ajax.core :as ajax]
            [clojure.string :as s]))

(def global-interceptors
  [db/check-spec-interceptor])

(re-frame/reg-event-db
 :db/init
 global-interceptors
 (fn [db _]
   (merge db
          {::db/session {:page :credentials
                         :token (stor/get-item "lrs-jwt")}
           ::db/login {:username "username"
                       :password "password"}
           ::db/credentials []
           ::db/browser {:content nil
                         :address nil
                         :credential nil}})))

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
 :session/set-page
 global-interceptors
 (fn [{:keys [db]} [_ page]]
   {:db (assoc-in db [::db/session :page] page)}))

(re-frame/reg-event-fx
 :browser/load-xapi
 global-interceptors
 (fn [_ [_ {:keys [path params]}]]
   (let [xapi-url (httpfn/build-xapi-url path params)]
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

(re-frame/reg-fx
 :store-token
 (fn [token]
   (if token
     (stor/set-item! "lrs-jwt" token)
     (stor/remove-item! "lrs-jwt"))))

(re-frame/reg-event-fx
 :session/set-token
 global-interceptors
 (fn [{:keys [db]} [_ token]]
   {:db (assoc-in db [::db/session :token] token)
    :store-token token}))

(re-frame/reg-event-db
 :login/error
 global-interceptors
 (fn [db [_ error]]
   (assoc-in db [::db/login :error] error)))

(re-frame/reg-event-fx
 :session/authenticate
 global-interceptors
 (fn [_ _]
   {:http-xhrio {:method          :post
                 :uri             (httpfn/serv-uri "/admin/account/login")
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          @(re-frame/subscribe [:db/get-login])
                 :on-success      [:login/success-handler]
                 :on-failure      [:login/login-failure]}}))

(re-frame/reg-event-fx
 :login/success-handler
 global-interceptors
 (fn [{:keys [db]} [_ {:keys [json-web-token]}]]
   {:dispatch [:session/set-token json-web-token]}))

(re-frame/reg-event-db
 :session/login-failure
 global-interceptors
 (fn [db [_ {:keys [body]}]]
   (assoc-in db [::db/login :error] body)))


(re-frame/reg-event-db
 :credentials/set-credentials
 global-interceptors
 (fn [db [_ credentials]]
   (assoc db ::db/credentials credentials)))

(re-frame/reg-event-fx
 :credentials/load-credentials
 global-interceptors
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             (httpfn/serv-uri "/admin/creds")
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
 (fn [{:keys [db]} [_ credential]]
   {:http-xhrio {:method          :put
                 :uri             (httpfn/serv-uri "/admin/creds")
                 :params          credential
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/load-credentials]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :credentials/create-credential
 global-interceptors
 (fn [{:keys [db]} [_ credential]]
   {:http-xhrio {:method          :post
                 :uri             (httpfn/serv-uri "/admin/creds")
                 :params          credential
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/load-credentials]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :credentials/delete-credential
 global-interceptors
 (fn [{:keys [db]} [_ credential]]
   {:http-xhrio {:method          :delete
                 :uri             (httpfn/serv-uri "/admin/creds")
                 :params          credential
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:credentials/load-credentials]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :browser/update-credential
 global-interceptors
 (fn [{:keys [db]} [_ key]]
   (let [credential (first (filter  #(= key (:api-key %))
                                    @(re-frame/subscribe [:db/get-credentials])))]
     (when credential
       {:db (assoc-in db [::db/browser :credential] credential)
        :dispatch [:browser/load-xapi]}))))

(re-frame/reg-event-fx
 :server-error
 (fn [{:keys [db]} [_ {:keys [body status]}]]
   (println status)
   (println body)))
