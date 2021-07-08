(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.core
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [com.yetanalytics.lrs-admin-ui.subs]
   [com.yetanalytics.lrs-admin-ui.handlers]
   [re-frame.core :refer [dispatch-sync]]
   [com.yetanalytics.lrs-admin-ui.views :as views]))

(defn mount [el]
  (println "Rendering View")
  (rdom/render [views/app] el))

(defn mount-app-element []
  (when-let [el (gdom/getElement "app")]
    (dispatch-sync [:db/init])
    (mount el)))

(mount-app-element)

(defn ^:after-load on-reload []
  (mount-app-element))
