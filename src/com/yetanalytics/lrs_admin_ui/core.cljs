(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.core
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [re-frame.core :refer [dispatch-sync]]
   [com.yetanalytics.lrs-admin-ui.subs]
   [com.yetanalytics.lrs-admin-ui.handlers]
   [com.yetanalytics.lrs-admin-ui.views :as views]))

(set! *warn-on-infer* true)

(defn mount [el]
  (println "Rendering View")
  (rdom/render [views/app] el))

(defn mount-app-element []
  (when-let [el (gdom/getElement "app")]
    (mount el)))

(defn ^:export init [server-host]
  (dispatch-sync [:db/init server-host])
  (mount-app-element))

(defn ^:after-load on-reload []
  (mount-app-element))
