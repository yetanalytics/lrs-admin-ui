(ns com.yetanalytics.lrs-admin-ui.views.data-management
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.lrs-admin-ui.views.data-management.delete-actor :refer [delete-actor]]))

(defn data-management []
  [:div {:class "left-content-wrapper"}
   ;; Delete Actor
   [:h2 {:class "content-title"}
    @(subscribe [:lang/get :datamgmt.title])]
   [delete-actor]
   [:div {:class "h-divider"}]
   ;; Download CSV
   [:h4 {:class "content-title"}
    @(subscribe [:lang/get :datamgmt.download.title])]
   [:input {:type "button",
            :class "btn-brand-bold",
            :on-click #(dispatch [:csv/auth-and-download])
            :value @(subscribe [:lang/get :datamgmt.download.button])}]])
