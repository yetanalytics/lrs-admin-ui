(ns com.yetanalytics.lrs-admin-ui.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [clojure.pprint :refer [pprint]]))

(reg-sub
 :db/get-db
 (fn [db _]
   db))

(reg-sub
 :db/get-session
 (fn [db _]
   (::db/session db)))

(reg-sub
 :session/get-page
 (fn [_ _]
   (subscribe [:db/get-session]))
 (fn [session _]
   (:page session)))

(reg-sub
 :session/get-token
 (fn [_ _]
   (subscribe [:db/get-session]))
 (fn [session _]
   (println session)
   (:token session)))

(reg-sub
 :db/get-login
 (fn [db _]
   (::db/login db)))

(reg-sub
 :login/get-username
 (fn [_ _]
   (subscribe [:db/get-login]))
 (fn [login _]
   (:username login)))

(reg-sub
 :login/get-password
 (fn [_ _]
   (subscribe [:db/get-login]))
 (fn [login _ ]
   (:password login)))

(reg-sub
 :login/get-error
 (fn [_ _]
   (subscribe [:db/get-login]))
 (fn [login _]
   (:error login)))

(reg-sub
 :db/get-credentials
 (fn [db _]
   (::db/credentials db)))
