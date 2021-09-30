(ns com.yetanalytics.lrs-admin-ui.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [com.yetanalytics.lrs-admin-ui.functions.http :refer [extract-params]]))

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
   (:token session)))

(reg-sub
 :session/get-username
 (fn [_ _]
   (subscribe [:db/get-session]))
 (fn [session _]
   (:username session)))

(reg-sub
 :notifications/get-notifications
 (fn [db _]
   (::db/notifications db)))


(reg-sub
 :db/get-login
 (fn [db _]
   (::db/login db)))

(reg-sub
 :db/get-accounts
 (fn [db _]
   (::db/accounts db)))

(reg-sub
 :db/get-new-account
 (fn [db _]
   (::db/new-account db)))

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
 (fn [login _]
   (:password login)))

(reg-sub
 :db/get-credentials
 (fn [db _]
   (::db/credentials db)))

(reg-sub
 :credentials/get-credential
 (fn [_ _]
   (subscribe [:db/get-credentials]))
 (fn [credentials [_ idx]]
   (get-in credentials [idx])))

(reg-sub
 :db/get-browser
 (fn [db _]
   (::db/browser db)))

(reg-sub
 :browser/get-content
 (fn [_ _]
   (subscribe [:db/get-browser]))
 (fn [browser _]
   (:content browser)))

(reg-sub
 :browser/get-address
 (fn [_ _]
   (subscribe [:db/get-browser]))
 (fn [browser _]
   (:address browser)))

(reg-sub
 :browser/get-credential
 (fn [_ _]
   (subscribe [:db/get-browser]))
 (fn [browser _]
   (:credential browser)))
