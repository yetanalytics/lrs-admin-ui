(ns com.yetanalytics.lrs-admin-ui.views.status.vis
  (:require [reagent.core :as r]
            [victory :refer [VictoryChart
                             VictoryAxis
                             VictoryPie
                             VictoryScatter
                             VictoryTheme
                             VictoryTooltip]]))

(def chart
  (r/adapt-react-class VictoryChart))

(def axis
  (r/adapt-react-class VictoryAxis))

(def pie
  (r/adapt-react-class VictoryPie))

(def scatter
  (r/adapt-react-class VictoryScatter))

(def tooltip
  (r/adapt-react-class VictoryTooltip))

(def theme VictoryTheme)
