(ns com.yetanalytics.lrs-admin-ui.views.data-management
  (:require [com.yetanalytics.lrs-admin-ui.views.delete-actor :refer [delete-actor]]))

(defn data-management []
      [:div {:class "left-content-wrapper"}
       [:h2 {:class "content-title"}
        "Data Management"]
       [delete-actor]])
