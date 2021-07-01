(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [com.yetanalytics.lrs-admin-ui.views.header :refer [header]]
   [com.yetanalytics.lrs-admin-ui.views.main :refer [main]]
   [com.yetanalytics.lrs-admin-ui.views.footer :refer [footer]]))

(defonce app-state (atom {:text "Hello world!"}))

(defn app []
  [:div
   [header]
   [main]
   [footer]])
