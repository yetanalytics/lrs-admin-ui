(ns com.yetanalytics.lrs-admin-ui.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [com.yetanalytics.lrs-admin-ui.input :as i]
            [com.yetanalytics.lrs-admin-ui.spec.reaction-edit]
            [clojure.spec.alpha :as s :include-macros true]
            com.yetanalytics.lrs-admin-ui.subs.browser
            com.yetanalytics.lrs-admin-ui.subs.reaction
            com.yetanalytics.lrs-admin-ui.subs.status))

(reg-sub
 :db/get-db
 (fn [db _]
   db))

(reg-sub
 :db/get-session
 (fn [db _]
   (::db/session db)))

(reg-sub
 ::db/proxy-path
 (fn [db _]
   (::db/proxy-path db)))

(reg-sub
 :db/language
 (fn [db _]
   (::db/language db)))

(reg-sub
 :db/pref-lang
 (fn [db _]
   (::db/pref-lang db)))

(reg-sub
 :db/stmt-get-max
 (fn [db _]
   (::db/stmt-get-max db)))

(reg-sub
 :lang/get
 :<- [:db/language]
 :<- [:db/pref-lang]
 (fn [[language pref-lang] [_ key]]
   (let [langmap (get language key)]
     (or (get langmap pref-lang)
         (get langmap :en-US)
         (get langmap (-> langmap keys first))))))

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
 :db/update-password
 (fn [db _]
   (::db/update-password db)))

(reg-sub
 :update-password/old-password
 :<- [:db/update-password]
 (fn [update-password _]
   (:old-password update-password)))

(reg-sub
 :update-password/new-password
 :<- [:db/update-password]
 (fn [update-password _]
   (:new-password update-password)))

(reg-sub
 :update-password/valid?
 :<- [:db/update-password]
 (fn [update-password _]
   (s/valid? ::i/valid-update-password update-password)))

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

;; Delete Actor

(reg-sub
 :delete-actor/enabled?
 (fn [db _]
   (::db/enable-admin-delete-actor db false)))

;; Dialog

(reg-sub
 :dialog/data
 (fn [db _]
   (::db/dialog-data db)))
