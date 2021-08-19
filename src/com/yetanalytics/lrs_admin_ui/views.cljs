(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views
  (:require
   [goog.dom :as gdom]
   [re-frame.core :refer [subscribe]]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [com.yetanalytics.lrs-admin-ui.views.header :refer [header]]
   [com.yetanalytics.lrs-admin-ui.views.main :refer [main]]
   [com.yetanalytics.lrs-admin-ui.views.footer :refer [footer]]
   [com.yetanalytics.lrs-admin-ui.views.login :refer [login]]))



(defn app []
  (let [token @(subscribe [:session/get-token])]
    (cond
      ;;TODO: Verify token expiration with jwt lib
      (= token nil)
      [:div
       [login]
       [footer]]
      :else
      [:div
       [header]
       [main]
       [footer]])))
