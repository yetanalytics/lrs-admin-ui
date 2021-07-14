(ns com.yetanalytics.lrs-admin-ui.handlers
  (:require [re-frame.core  :as re-frame]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [day8.re-frame.http-fx]
            [com.yetanalytics.lrs-admin-ui.functions.http :as httpfn]
            [ajax.core :as ajax]))

(def global-interceptors
  [db/check-spec-interceptor])

(re-frame/reg-event-db
 :db/init
 global-interceptors
 (fn [db _]
   (-> db
       (assoc ::db/session {:page :credentials
                            :token nil})
       (assoc ::db/login {:username "username"
                          :password "password"})
       (assoc ::db/credentials []))))

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
 :session/set-page
 global-interceptors
 (fn [db [_ page]]
   (assoc-in db [::db/session :page] page)))

(re-frame/reg-event-db
 :session/set-token
 global-interceptors
 (fn [db [_ token]]
   (assoc-in db [::db/session :token] token)))

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
                 :on-success      [:login/success]
                 :on-failure      [:login/failure]}}))

(re-frame/reg-event-fx
 :login/success
 global-interceptors
 (fn [{:keys [db]} [_ res]]
   {:db (assoc-in db [::db/session :token] (:json-web-token res))
    ;;do loady stuff
    :dispatch [:credentials/load-credentials]}))

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
 :server-error
 (fn [{:keys [db]} [_ {:keys [body status]}]]
   (println status)
   (println body)))
