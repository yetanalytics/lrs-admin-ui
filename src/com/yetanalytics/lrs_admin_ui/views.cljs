(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]))

(defonce app-state (atom {:text "Hello world!"}))

(defn app []
  [:div
   [:h1 (:text @app-state)]
   [:h3 "root of the application framework!"]])
