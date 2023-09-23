(ns com.yetanalytics.lrs-admin-ui.views.special-actions
  (:require [com.yetanalytics.lrs-admin-ui.views.delete-actor :refer [delete-actor]]))

(defn special-actions []
      [:div {:class "left-content-wrapper"}
       [:h2 {:class "content-title"}
        "Special Actions"]
       [delete-actor]])
