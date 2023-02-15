(ns com.yetanalytics.lrs-admin-ui.views.status.vis
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe]]
            [victory :refer [VictoryChart
                             VictoryAxis
                             VictoryZoomContainer
                             VictoryPie
                             VictoryLine
                             VictoryBar
                             VictoryScatter
                             VictoryTheme
                             VictoryTooltip]]
            [goog.string :refer [format]]
            [goog.string.format]))

(def chart
  (r/adapt-react-class VictoryChart))

(def axis
  (r/adapt-react-class VictoryAxis))

(def zoom
  (r/adapt-react-class VictoryZoomContainer))

(def pie
  (r/adapt-react-class VictoryPie))

(def scatter
  (r/adapt-react-class VictoryScatter))

(def tooltip
  (r/adapt-react-class VictoryTooltip))

(defn timeline
  [label
   sub-qvec]
  [:div.vis-timeline
   [:h4 label]
   [chart
    {:min-domain {:y 0}
     :container-component (r/as-element [zoom])
     :domain-padding 10
     :theme (.-material VictoryTheme)}
    [scatter
     {:standalone false
      :label-component (r/as-element
                        [tooltip
                         {:style {:font-size 8}}])
      :labels (fn [c]
                (let [datum (.-datum c)
                      x (.-x datum)
                      y (.-y datum)]
                  (format "%s: %s"
                          (.toLocaleString x) y)))
      :data @(subscribe sub-qvec)}]
    [axis
     {:standalone false
      :dependent-axis true
      :orientation "left"
      :tick-format
      (fn [x]
        (when (int? x)
          (str x)))
      :style
      {:tick-labels {:font-size 8}}}]
    [axis
     {:standalone false
      :dependent-axis false
      :scale "time"
      :orientation "bottom"
      :tick-format
      (fn [x]
        (.toLocaleString (js/Date. x)))
      :style
      {:tick-labels {:font-size 6
                     :angle 60
                     :text-anchor "start"}}}]]])
