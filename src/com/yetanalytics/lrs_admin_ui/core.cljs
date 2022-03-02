(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.core
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [com.yetanalytics.lrs-admin-ui.subs]
   [com.yetanalytics.lrs-admin-ui.handlers]
   [re-frame.core :refer [dispatch-sync]]
   [com.yetanalytics.lrs-admin-ui.views :as views]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]))

(set! *warn-on-infer* true)

(defn mount [el]
  (println "Rendering View")
  (rdom/render [views/app] el))

(defn mount-app-element []
  (when-let [el (gdom/getElement "app")]
    (mount el)))

(defn ^:export init [& [?js-config]]
  (let [{:keys [server-host]} (if ?js-config
                                (cske/transform-keys
                                 csk/->kebab-case-keyword
                                 (js->clj ?js-config))
                                {})]
    (dispatch-sync [:db/init server-host])
    (mount-app-element)))

(defn ^:after-load on-reload []
  (mount-app-element))
