(ns com.yetanalytics.lrs-admin-ui.views.status
  (:require
   [re-frame.core :refer [subscribe]]))

(defn status
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "LRS Status"]

   ])
