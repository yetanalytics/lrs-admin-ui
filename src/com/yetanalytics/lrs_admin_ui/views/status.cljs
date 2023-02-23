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
  [vis-key label sub-qvec]
  (let [value @(subscribe sub-qvec)]
    [:div.big-number
   [:div.big-number-value
    {:class (if @(subscribe [:status/loading? vis-key])
              "spinner"
              "")}
    (or value " ")]
   [:div.big-number-label
    label]]))

(defn timestamp
  [vis-key label sub-qvec nil-text]
  (let [value @(subscribe sub-qvec)]
    [:div.timestamp
     [:div.timestamp-label
      label]
     [:div.timestamp-value
      {:class (if @(subscribe [:status/loading? vis-key])
                "spinner"
                "")}
      (or value nil-text)]]))

(defn refresh-button
  []
  [:div.status-refresh-button
   [:input.btn-blue-bold
    {:type "button"
     :value "REFRESH"
     :on-click #(dispatch [:status/get-all-data])}]])

(defn title-loading-spinner
  [vis-key]
  [:span.vis-title-loading-spinner
   {:class (if @(subscribe [:status/loading? vis-key])
             "spinner"
             "")}])

(defn- get-datum-x-y
  [c]
  (let [datum (aget c "datum")
        x (aget datum "x")
        y (aget datum "y")]
    {:x x
     :y y}))

(defn platform-bar
  []
  (let [vis-key "platform-frequency"
        data @(subscribe [:status.data/platform-frequency])
        loading? @(subscribe [:status/loading? vis-key])]
    [:div.vis-bar
     [:h4 "PLATFORMS"
      [title-loading-spinner vis-key]]
     (cond
       (not-empty data)
       [vis/chart
        {:theme (aget vis/theme "material")
         :padding {:top 10 :left 40 :bottom 30}
         :domain-padding {:x 40}}
        [vis/bar
         {:standalone false
          :data data
          :sort-key "y"
          :sort-order "descending"
          :style {:data {:fill (fn [c]
                                 (-> c
                                     (aget "datum")
                                     (aget "fill")))}}}]
        [vis/axis
         {:standalone false
          :dependent-axis true
          :orientation "left"
          :tick-format (fn [y] (format "%d%" y))}]
        [vis/axis
         {:standalone false
          :dependent-axis false
          :orientation "bottom"}]]
       loading?
       [:div]

       :else
       [:div.no-data "No Statement Data"])]))

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
       :domain-padding 10
       :theme (aget vis/theme "material")
       ;; :height 200
       :width 500
       :padding {:top 10 :left 60 :right 85 :bottom 80}}
      [vis/scatter
       {:standalone false
        :label-component (r/as-element
                          [vis/tooltip])
        :labels (fn [c]
                  (let [{:keys [x y]} (get-datum-x-y c)]
                    (format "%s: %s Statements"
                            (x-tick-format x-unit x) y)))
        :data @(subscribe [:status.data.timeline/data])
        :style {:data {:fill "#137BCE"}}}]
      [vis/axis
       {:standalone false
        :dependent-axis true
        :orientation "left"
        :tick-format
        (fn [y]
          (when (int? y)
            (str y)))}]
      [vis/axis
       {:standalone false
        :dependent-axis false
        :scale "time"
        :orientation "bottom"
        :tick-format
        (fn [x]
          (x-tick-format x-unit (js/Date. x)))
        :style
        {:tick-labels {:angle 30
                       :text-anchor "start"}}}]]]))

(defn timeline
  []
  [:div.vis-timeline
   [:h4 "TIMELINE"
    [title-loading-spinner "timeline"]]
   [timeline-controls]
   [timeline-chart]])

(defn status
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "LRS Monitor"]
   [refresh-button]
   [:div.status-vis-row
    [big-number
     "statement-count"
     "STATEMENTS"
     [:status.data/statement-count]]
    [big-number
     "actor-count"
     "ACTORS"
     [:status.data/actor-count]]]
   [:div.status-vis-row
    [timestamp
     "last-statement-stored"
     "LAST STATEMENT AT"
     [:status.data/last-statement-stored-locale]
     "-"]]
   [:div.status-vis-row
    [timeline]
    [platform-bar]]])
