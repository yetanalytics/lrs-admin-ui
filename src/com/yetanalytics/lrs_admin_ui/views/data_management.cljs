(ns com.yetanalytics.lrs-admin-ui.views.data-management
  (:require [re-frame.core :refer [subscribe]]
            [com.yetanalytics.lrs-admin-ui.views.data-management.delete-actor :refer [delete-actor]]))

(defn data-management []
      [:div {:class "left-content-wrapper"}
       [:h2 {:class "content-title"}
        @(subscribe [:lang/get :datamgmt.title])]
       [delete-actor]])
