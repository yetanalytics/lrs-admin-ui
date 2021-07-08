(ns com.yetanalytics.lrs-admin-ui.handlers
  (:require [re-frame.core  :as re-frame]
            [com.yetanalytics.lrs-admin-ui.db :as db]))

(def global-interceptors
  [db/check-spec-interceptor])

(re-frame/reg-event-db
 :db/init
 global-interceptors
 (fn [db _]
   (assoc db ::db/session {:page :credentials})))

(re-frame/reg-event-db
 :session/set-page
 global-interceptors
 (fn [db [_ page]]
   (assoc-in db [::db/session :page] page)))
