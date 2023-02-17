(ns com.yetanalytics.lrs-admin-ui.views.status
  (:require
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [clojure.string :as cs]
   [com.yetanalytics.lrs-admin-ui.functions :as fns]
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
     [:div.timestamp-label
      label]
     [:div.timestamp-value
      {:class (if value
                ""
                "spinner")}
      (or value " ")]]))

(defn refresh-button
  []
  [:div.status-refresh-button
   [:input.btn-blue-bold
    {:type "button"
     :value "REFRESH"
     :on-click #(dispatch [:status/get-all-data])}]])

(defn platform-pie
  []
  [:div.vis-pie
   [:h4 "PLATFORMS"]
   [vis/pie
    {:theme (aget vis/theme "material")
     :data @(subscribe [:status.data/platform-frequency])
     :labels (fn [c]
               (let [datum (aget c "datum")
                     x (aget datum "x")
                     y (aget datum "y")]
                 (format "%s: %s"
                         x y)))}]])

(defn timeline-select-unit
  []
  (let [current-unit @(subscribe [:status.params/timeline-unit])]
    [:div.vis-timeline-controls-select-unit
     [:label
      {:for "timeline-select-unit"}
      "Time Unit"]
     (into [:select
            {:id "timeline-select-unit"
             :value current-unit
             :on-change #(dispatch [:status/set-timeline-unit
                                    (fns/ps-event-val %)])}]
           (for [unit ["year"
                       "month"
                       "day"
                       "hour"
                       "minute"
                       "second"]]
             [:option
              {:value unit
               :key (str "unit-" unit)}
              (cs/capitalize unit)]))]))

(defn- pick-datetime
  [input-id
   label
   value
   on-change
   min-datetime
   & [max-datetime]]
  [:div.vis-timeline-controls-pick-datetime
   [:label
    {:for input-id}
    label]
   [:input
    (cond-> {:id input-id
             :type "datetime-local"
             :value value
             :on-change on-change
             :min min-datetime}
      max-datetime (assoc :max max-datetime))]])

(defn timeline-pick-since
  []
  [pick-datetime
   "timeline-pick-since"
   "Since"
   @(subscribe [:status.params/timeline-since-local])
   #(dispatch [:status/set-timeline-since
               (fns/ps-event-val %)])
   "1970-01-01T00:00:00"
   @(subscribe [:status.params/timeline-until-local])])

(defn timeline-pick-until
  []
  [pick-datetime
   "timeline-pick-until"
   "Until"
   @(subscribe [:status.params/timeline-until-local])
   #(dispatch [:status/set-timeline-until
               (fns/ps-event-val %)])
   @(subscribe [:status.params/timeline-since-local])])

(defn timeline-controls
  []
  [:div.vis-timeline-controls
   [timeline-select-unit]
   [timeline-pick-since]
   [timeline-pick-until]])

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

(defn timeline-chart
  []
  (let [x-unit @(subscribe [:status.params/timeline-unit])]
    [:div.vis-timeline-chart
     [vis/chart
      {:domain @(subscribe [:status.data.timeline/domain])
       :min-domain {:y 0}
       :container-component (r/as-element [vis/zoom
                                           {:zoom-dimension "x"}])
       :domain-padding 10
       :theme (aget vis/theme "material")
       :height 200
       :padding {:top 10 :left 40 :right 30 :bottom 70}}
      [vis/scatter
       {:standalone false
        :label-component (r/as-element
                          [vis/tooltip
                           {:style {:font-size 8}}])
        :labels (fn [c]
                  (let [datum (aget c "datum")
                        x (aget datum "x")
                        y (aget datum "y")]
                    (format "%s: %s Statements"
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
        {:tick-labels {:font-size 6}}}]
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

(defn timeline
  []
  [:div.vis-timeline
   [:h4 "TIMELINE"]
   [timeline-controls]
   [timeline-chart]])

(defn status
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "LRS Status"]
   [refresh-button]
   [:div.status-vis-row
    [big-number "STATEMENTS" [:status.data/statement-count]]
    [big-number "ACTORS" [:status.data/actor-count]]]
   [:div.status-vis-row
    [timestamp "LAST STATEMENT AT" [:status.data/last-statement-stored-locale]]]
   [:div.status-vis-row
    [timeline]]
   [:div.status-vis-row
    [platform-pie]]])
