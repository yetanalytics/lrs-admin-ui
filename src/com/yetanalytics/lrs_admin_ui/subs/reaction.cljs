(ns com.yetanalytics.lrs-admin-ui.subs.reaction
  (:require [re-frame.core      :refer [reg-sub]]
            [clojure.spec.alpha :as s :include-macros true]
            [com.yetanalytics.lrs-admin-ui.db                 :as db]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]))


;; Reactions

;; Are we viewing the list, an individual activiy, editing or creating?
(reg-sub
 :reaction/mode
 :<- [:reaction/editing]
 :<- [:reaction/focus-id]
 (fn [[editing
       focus]]
   (cond
     editing (if (:id editing)
               :edit
               :new)
     focus :focus
     :else :list)))

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
