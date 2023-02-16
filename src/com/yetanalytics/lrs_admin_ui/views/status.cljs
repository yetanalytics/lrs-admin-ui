(ns com.yetanalytics.lrs-admin-ui.views.status
  (:require
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [com.yetanalytics.lrs-admin-ui.views.status.vis :as vis]
   [goog.string :refer [format]]
   [goog.string.format]))

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

(def unit-for
  "Mapping of timeline bucket time unit to suitable FOR sql substring arg."
  {"year"   4
   "month"  7
   "day"    10
   "hour"   13
   "minute" 16
   "second" 19})

(defn- x-tick-format
  [unit js-date]
  (subs (.toISOString js-date) 0 (get unit-for unit)))

(defn timeline
  []
  (let [x-unit @(subscribe [:status.params/timeline-unit])]
    [:div.vis-timeline
     [:h4 "TIMELINE"]
     [vis/chart
      {:domain @(subscribe [:status.data.timeline/domain])
       :min-domain {:y 0}
       :container-component (r/as-element [vis/zoom
                                           {:zoom-dimension "x"}])
       :domain-padding 10
       :theme (.-material vis/theme)}
      [vis/scatter
       {:standalone false
        :label-component (r/as-element
                          [vis/tooltip
                           {:style {:font-size 8}}])
        :labels (fn [c]
                  (let [datum (.-datum c)
                        x (.-x datum)
                        y (.-y datum)]
                    (format "%s: %s"
                            (x-tick-format x-unit x) y)))
        :data @(subscribe [:status.data.timeline/data])}]
      [vis/axis
       {:standalone false
        :dependent-axis true
        :orientation "left"
        :tick-format
        (fn [y]
          (when (int? y)
            (str y)))
        :style
        {:tick-labels {:font-size 8}}}]
      [vis/axis
       {:standalone false
        :dependent-axis false
        :scale "time"
        :orientation "bottom"
        :tick-format
        (fn [x]
          (x-tick-format x-unit (js/Date. x)))
        :style
        {:tick-labels {:font-size 6
                       :angle 60
                       :text-anchor "start"}}}]]]))


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
    [timeline]]])
