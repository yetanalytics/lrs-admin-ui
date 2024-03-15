(ns com.yetanalytics.lrs-admin-ui.views.data-management.delete-actor
  (:require [re-frame.core :refer [dispatch-sync subscribe]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [reagent.core :as r]))

(defn inp->ifi [type inp]
  (case type
    "account" (str "account::" (:name inp) "@" (:home-page inp))
    "mbox" (str "mbox::" inp)
    "mbox_sha1sum" (str "mbox_sha1sum::" inp)
    "openid" (str "openid::" inp)))

(defn labeled-input [label ratm]
  [:div.section-pad.labeled-input
   [:div [:span.font-monospace label] ":"]
   [:input.round {:type "text"
            :value @ratm
            :on-change #(reset! ratm (fns/ps-event-val %))}]])

(defn delete-actor []
  (let [ifi-types ["mbox" "mbox_sha1sum" "openid" "account"]
        ifi-type (r/atom (first ifi-types))
        input (r/atom nil)]
    (fn []
      [:div 
       [:h4 {:class "content-title"}
        @(subscribe [:lang/get :datamgmt.delete.title])]
       [:div.section-pad (into [:select {:on-change #(do
                                         (reset! input nil)
                                         (reset! ifi-type (fns/ps-event-val %)))}]
                 (for [k ifi-types]
                   [:option {:value k :key k} k]))]
       
       (case @ifi-type "account"
             [:<>
              [labeled-input "name" (r/cursor input [:name])]
              [labeled-input "homePage" (r/cursor input [:home-page])]]
             [labeled-input (name @ifi-type) input])
       [:div.section-pad [:input {:type "button",
                                  :class "btn-blue-bold",
                                  :on-click  #(dispatch-sync [:delete-actor/delete-actor (inp->ifi @ifi-type @input)])
                                  :value @(subscribe [:lang/get :datamgmt.delete.button])}]]])))
