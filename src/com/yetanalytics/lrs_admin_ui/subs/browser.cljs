(ns com.yetanalytics.lrs-admin-ui.subs.browser
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.yetanalytics.lrs-admin-ui.db :as db]))

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

(reg-sub
 :browser/get-more-link
 (fn [_ _]
   (subscribe [:db/get-browser]))
 (fn [{:keys [more-link]} _]
   more-link))

(reg-sub
 :browser/get-batch-size
 (fn [_ _]
   (subscribe [:db/get-browser]))
 (fn [{:keys [batch-size]} _]
   batch-size))

(reg-sub
 :browser/get-back-stack
 (fn [_ _]
   (subscribe [:db/get-browser]))
 (fn [{:keys [back-stack]} _]
   back-stack))
