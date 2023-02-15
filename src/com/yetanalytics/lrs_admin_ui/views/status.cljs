(ns com.yetanalytics.lrs-admin-ui.views.status
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [com.yetanalytics.lrs-admin-ui.views.status.vis :as vis]))

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

(defn timestamp
  [label sub-qvec]
  (let [value @(subscribe sub-qvec)]
    [:div.timestamp
     [:div.timestamp-value
      {:class (if value
                ""
                "spinner")}
      (or value " ")]
     [:div.timestamp-label
      label]]))

(defn refresh-button
  []
  [:div.status-refresh-button
   [:input.btn-blue-bold
    {:type "button"
     :value "REFRESH"
     :on-click #(dispatch [:status/get-data])}]])

(defn platform-pie
  []
  [:div.vis-pie
   [:h4 "PLATFORMS"]
   [vis/pie
    {:data @(subscribe [:status.data/platform-frequency])}]])

(defn status
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "LRS Status"]
   [refresh-button]
   [:div.status-vis-row
    [timestamp "LAST STATEMENT AT" [:status.data/last-statement-stored]]]
   [:div.status-vis-row
    [big-number "STATEMENTS" [:status.data/statement-count]]
    [big-number "ACTORS" [:status.data/actor-count]]]
   [:div.status-vis-row
    [platform-pie]]
   [:div.status-vis-row
    [vis/timeline
     "TIMELINE"
     [:status.data.timeline/data]]]])
