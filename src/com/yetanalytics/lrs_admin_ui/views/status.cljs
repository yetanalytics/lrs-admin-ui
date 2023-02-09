(ns com.yetanalytics.lrs-admin-ui.views.status
  (:require
   [re-frame.core :refer [subscribe dispatch]]))

(defn big-number
  [label sub-qvec]
  (let [value @(subscribe sub-qvec)]
    [:div.big-number
   [:div.big-number-value
    {:class (if value
              ""
              "spinner")}
    (or value " ")]
   [:div.big-number-label
    label]]))

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

   [big-number "STATEMENTS" [:status.data/statement-count]]
   [big-number "ACTORS" [:status.data/actor-count]]])
