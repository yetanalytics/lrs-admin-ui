(ns com.yetanalytics.lrs-admin-ui.views.status
  (:require
   [re-frame.core :refer [subscribe dispatch]]))

(defn big-number
  [label sub-qvec]
  [:div.big-number
   [:div.big-number-label
    label]
   [:div.big-number-value
    @(subscribe sub-qvec)]])

(defn refresh-button
  []
  [:input.btn-blue-bold
   {:type "button"
    :value "REFRESH"
    :on-click #(dispatch [:status/get-data])}])

(defn status
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "LRS Status"]
   [refresh-button]
   [big-number "Statement Count" [:status.data/statement-count]]])
