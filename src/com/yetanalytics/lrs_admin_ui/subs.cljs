(ns com.yetanalytics.lrs-admin-ui.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.yetanalytics.lrs-admin-ui.db :as db]))

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
 :session/get-display-name
 :<- [:session/get-username]
 :<- [:com.yetanalytics.re-oidc.user/profile]
 (fn [[username
       ?profile] _]
   (or (when-let [{:strs [name
                          nickname
                          preferred_username]} ?profile]
         (or name
             nickname
             preferred_username))
       username)))

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

(reg-sub
 :db/get-stmt-html-enabled
 (fn [db _]
   (::db/enable-statement-html db)))

;; OIDC State
(reg-sub
 :oidc/login-available?
 :<- [:com.yetanalytics.re-oidc/status]
 (fn [?status _]
   (and ?status
        (not= ?status :loaded))))

(reg-sub
 :oidc/enabled?
 (fn [db _]
   (::db/oidc-auth db false)))

(reg-sub
 :oidc/local-admin-enabled?
 (fn [db _]
   (::db/oidc-enable-local-admin db false)))

;; Hide/show local login based on oidc-enable-local-admin
(reg-sub
 :oidc/show-local-login?
 :<- [:oidc/enabled?]
 :<- [:oidc/local-admin-enabled?]
 (fn [[oidc-enabled?
       local-admin-enabled?] _]
   (if oidc-enabled?
     local-admin-enabled?
     true)))

;; Showing the account mgmt nav is just an alias
(reg-sub
 :oidc/show-account-nav?
 :<- [:oidc/show-local-login?]
 (fn [show-local-login? _]
   show-local-login?))

;; Status Dashboard
(reg-sub
 :status/enabled?
 (fn [db _]
   (::db/enable-admin-status db false)))

(reg-sub
 :db/status
 (fn [db _]
   (::db/status db)))

(reg-sub
 :status/data
 :<- [:db/status]
 (fn [status _]
   (:data status)))

(reg-sub
 :status.data/statement-count
 :<- [:status/data]
 (fn [data _]
   (:statement-count data)))

(reg-sub
 :status.data/actor-count
 :<- [:status/data]
 (fn [data _]
   (:actor-count data)))

(reg-sub
 :status.data/last-statement-stored
 :<- [:status/data]
 (fn [data _]
   (:last-statement-stored data)))

(reg-sub
 :status.data/platform-frequency
 :<- [:status/data]
 (fn [data _]
   (mapv
    (fn [[platform count]]
      {:x platform
       :y count})
    (:platform-frequency data))))
