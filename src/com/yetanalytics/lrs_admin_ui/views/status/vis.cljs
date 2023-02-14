(ns com.yetanalytics.lrs-admin-ui.views.status.vis
  (:require [reagent.core :as r]
            [victory :refer [VictoryPie
                             VictoryLine
                             VictoryBar]]))

(def pie
  (r/adapt-react-class VictoryPie))

(def line
  (r/adapt-react-class VictoryLine))

(def bar
  (r/adapt-react-class VictoryBar))
