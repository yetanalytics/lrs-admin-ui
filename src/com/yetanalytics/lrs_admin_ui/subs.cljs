(ns com.yetanalytics.lrs-admin-ui.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [com.yetanalytics.lrs-admin-ui.functions.time :as t]
            [com.yetanalytics.lrs-admin-ui.input :as i]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-admin-ui.spec.csv-download]
            [com.yetanalytics.lrs-admin-ui.spec.reaction-edit]
            [clojure.spec.alpha :as s :include-macros true]))

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
 ::db/resource-base
 (fn [db _]
   (::db/resource-base db)))

(reg-sub
 :resources/image
 :<- [::db/proxy-path]
 :<- [::db/resource-base]
 (fn [[proxy-path resource-base] [_ image-name]]
   (str proxy-path resource-base "images/" image-name)))

(reg-sub
 :resources/icon
 :<- [::db/proxy-path]
 :<- [::db/resource-base]
 (fn [[proxy-path resource-base] [_ icon-name]]
   (str proxy-path resource-base "images/icons/" icon-name)))

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

;; Download CSV

(reg-sub
 :csv/property-paths
 (fn [db _]
   (get-in db [::db/csv-download-properties :property-paths])))

(reg-sub
 :csv/property-path-valid
 :<- [:csv/property-paths]
 (fn [property-paths _]
   (s/valid? :validation/property-paths property-paths)))

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
 :status/params
 :<- [:db/status]
 (fn [status _]
   (:params status)))

(reg-sub
 :status.params/timeline-unit
 :<- [:status/params]
 (fn [params _]
   (:timeline-unit params "day")))

(reg-sub
 :status.params/timeline-since
 :<- [:status/params]
 (fn [params _]
   (:timeline-since params (t/timeline-since-default))))

(reg-sub
 :status.params/timeline-since-local
 :<- [:status.params/timeline-since]
 (fn [since _]
   (t/utc->local-datetime since)))

(reg-sub
 :status.params/timeline-until
 :<- [:status/params]
 (fn [params _]
   (:timeline-until params (t/timeline-until-default))))

(reg-sub
 :status.params/timeline-until-local
 :<- [:status.params/timeline-until]
 (fn [until _]
   (t/utc->local-datetime until)))

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
 :status.data/last-statement-stored-locale
 :<- [:status.data/last-statement-stored]
 (fn [last-stored _]
   (when last-stored
     (.toLocaleString (js/Date. last-stored)))))

(def bar-colors
  ["#137BCE"
   "#BAD54C"
   "#20956A"
   "#405EA7"
   "#D88638"
   "#6D4B9B"])

(reg-sub
 :status.data/platform-frequency
 :<- [:status/data]
 (fn [data _]
   (let [freqs (:platform-frequency data {})
         total (reduce + 0 (vals freqs))]
     (mapv
      (fn [[[platform count] color]]
        {:x platform
         :y (-> count
                (/ total)
                (* 100)
                int)
         :fill color})
      (map vector freqs (cycle bar-colors))))))

(reg-sub
 :status.data.timeline/data
 :<- [:status/data]
 (fn [data _]
   (mapv
    (fn [{:keys [stored count]}]
      {:x (js/Date. stored)
       :y count})
    (:timeline data))))

(reg-sub
 :status.data.timeline/domain
 :<- [:status.params/timeline-since]
 :<- [:status.params/timeline-until]
 :<- [:status.data.timeline/data]
 (fn [[since until data] _]
   {:x [;; Make sure the domain min includes all data present
        ;; this prevents a blank timeline in some cases
        (js/Date.
         (if (not-empty data)
           (min
            (-> data first :x .getTime)
            (.parse js/Date since))
           since))
        (js/Date. until)]}))

(reg-sub
 :status/loading-map
 :<- [:db/status]
 (fn [status _]
   (:loading status {})))

(reg-sub
 :status/loading?
 :<- [:status/loading-map]
 (fn [loading-map [_ loading-k]]
   (true? (get loading-map loading-k))))

;; Delete Actor

(reg-sub
 :delete-actor/enabled?
 (fn [db _]
   (::db/enable-admin-delete-actor db false)))

;; Reactions

(reg-sub
 :reaction/enabled?
 (fn [db _]
   (::db/enable-reactions db)))

(reg-sub
 :reaction/list
 (fn [db _]
   (::db/reactions db [])))

(reg-sub
 :reaction/focus-id
 (fn [{focus-id ::db/reaction-focus} _]
   focus-id))

(reg-sub
 :reaction/focus
 :<- [:reaction/list]
 :<- [:reaction/focus-id]
 (fn [[reaction-list focus-id] _]
   (some
    (fn [{:keys [id] :as reaction}]
      (when (= focus-id id)
        (rfns/db->focus-form reaction)))
    reaction-list)))

(reg-sub
 :reaction/editing
 (fn [db _]
   (::db/editing-reaction db)))

(reg-sub
 :reaction/edit-dirty?
 :<- [:reaction/list]
 :<- [:reaction/editing]
 (fn [[reaction-list
       {:keys [id] :as editing}]]
   (and (some? editing)
        (or (nil? id)
            (not= editing
                  (some
                   (fn [{r-id :id
                         :as  reaction}]
                     (when (= r-id id)
                       (rfns/focus->edit-form reaction)))
                   reaction-list))))))

(reg-sub
 :reaction/edit-condition-names
 :<- [:reaction/editing]
 (fn [{{:keys [conditions]} :ruleset} _]
   (map :name conditions)))

(reg-sub
 :reaction/edit-template-json
 (fn [db _]
   (::db/editing-reaction-template-json db)))

(reg-sub
 :reaction/edit-template-errors
 (fn [db _]
   (::db/editing-reaction-template-errors db [])))

(reg-sub
 :reaction/edit-template-buffer
 :<- [:reaction/list]
 :<- [:reaction/editing]
 :<- [:reaction/edit-template-json]
 :<- [:reaction/edit-template-errors]
 (fn [[reaction-list editing json errors] _]
   (let [editing-id (:id editing)
         saved      (some
                     (fn [{:keys [id] :as reaction}]
                  (when (= editing-id id)
                    (get-in reaction [:ruleset :template])))
                reaction-list)]
     {:saved  (or saved {})
      :value  (get-in editing [:ruleset :template])
      :json   json
      :status (if (empty? errors) :valid :error)
      :errors errors})))

(reg-sub
 :reaction/edit-spec-errors
 :<- [:reaction/editing]
 (fn [editing _]
   (when editing
     (some-> (s/explain-data
              :validation/reaction
              editing)
             :cljs.spec.alpha/problems))))

(reg-sub
 :reaction/edit-spec-errors-map
 :<- [:reaction/edit-spec-errors]
 (fn [errors _]
   (reduce
    (fn [m problem]
      (update m (:in problem) (fnil conj []) problem))
    {}
    errors)))

(reg-sub
 :reaction/edit-spec-errors-in
 :<- [:reaction/edit-spec-errors-map]
 (fn [emap [_ in-path]]
   (get emap in-path)))

(reg-sub
 :reaction/edit-error?
 :<- [:reaction/edit-spec-errors]
 :<- [:reaction/edit-template-errors]
 (fn [[spec-errors template-errors] _]
   (boolean (or (some? spec-errors)
                (seq template-errors)))))

;; Dialog

(reg-sub
 :dialog/data
 (fn [db _]
   (::db/dialog-data db)))
