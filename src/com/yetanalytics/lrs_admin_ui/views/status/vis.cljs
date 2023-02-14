(ns com.yetanalytics.lrs-admin-ui.views.status.vis
  (:require [reagent.core :as r]
            [victory :refer [VictoryPie]]))

(def pie
  (r/adapt-react-class VictoryPie))
