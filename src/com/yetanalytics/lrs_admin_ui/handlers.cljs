(ns com.yetanalytics.lrs-admin-ui.handlers
  (:require [re-frame.core  :as re-frame]
            [com.yetanalytics.lrs-admin-ui.db :as db]))

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
 :session/set-page
 global-interceptors
 (fn [db [_ page]]
   (assoc-in db [::db/session :page] page)))

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
 :session/set-token
 global-interceptors
 (fn [db [_ token]]
   (assoc-in db [::db/session :token] token)))

(re-frame/reg-event-db
 :login/set-error
 global-interceptors
 (fn [db [_ error]]
   (assoc-in db [::db/login :error] error)))

(re-frame/reg-event-db
 :credentials/set-credentials
 global-interceptors
 (fn [db [_ credentials]]
   (assoc db ::db/credentials credentials)))
