(ns com.yetanalytics.lrs-admin-ui.functions.tooltip
  (:require [reagent.core    :as r]
            ["react-tooltip" :as rtt]
            [goog.object     :as gobject]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Base Tooltip Component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fancy-tooltip
  "This component initializes a `react-tooltip` react component. The typical
  usage is to embed this into a view alongside an element that you want to have
  a tooltip, with the `:id` attr of this component matching the `:data-for` attr of
  the target element. The `:data-tip` attr of the target element is what will be
  displayed. It is probably best to have one of these per target element,
  because otherwise they can conflict. The module has a lot of opts, which are
  described here:
  https://github.com/ReactTooltip/react-tooltip#options"
  [opts]
  [(r/adapt-react-class (gobject/get rtt "default"))
   opts])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tooltips
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tooltip-info
  "Expects :value and optionally :left?. value is a string which will display in
  the tooltip. :left? adds a style to float it left."
  [{:keys [value left? type]
    :or   {left? false
           type  "info"}}]
  (let [tooltip-id (str (random-uuid))]
    [:span {:class    (cond-> "tooltip-info"
                        left? (str " left"))
            :data-tip value
            :data-for tooltip-id}
     [fancy-tooltip {:id         tooltip-id
                     :type       type
                     :multiline  true
                     :effect     "solid"
                     :place      "right"
                     :class-name "tooltip-text"}]]))
