(ns com.yetanalytics.lrs-admin-ui.views.status.vis
  (:require [reagent.core :as r]
            [cljsjs.victory]))

;; TODO: This cannot use cljsjs. We need to get it working like OIDC client does for adv comp.
(def pie
  (r/adapt-react-class js/Victory.VictoryPie))
