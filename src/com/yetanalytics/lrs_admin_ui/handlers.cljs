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
       (assoc ::db/credentials [{:api-key "thing"
                                 :secret-key "thang"
                                 :scopes ["all", "read"]}
                                {:api-key "thing2"
                                 :secret-key "thang2"
                                 :scopes ["none"]}]))))

(re-frame/reg-event-db
 :session/set-page
 global-interceptors
 (fn [db [_ page]]
   (assoc-in db [::db/session :page] page)))
