(ns com.yetanalytics.lrs-admin-ui.views.status.vis
  (:require [reagent.core :as r]
            [victory-core :refer [VictoryTheme]]
            [victory-chart :refer [VictoryChart]]
            [victory-axis :refer [VictoryAxis]]
            [victory-bar :refer [VictoryBar]]
            [victory-scatter :refer [VictoryScatter]]
            [victory-tooltip :refer [VictoryTooltip]]))

(def chart
  (r/adapt-react-class VictoryChart))

(def axis
  (r/adapt-react-class VictoryAxis))

(def bar
  (r/adapt-react-class VictoryBar))

(def scatter
  (r/adapt-react-class VictoryScatter))

(def tooltip
  (r/adapt-react-class VictoryTooltip))

(def theme VictoryTheme)
