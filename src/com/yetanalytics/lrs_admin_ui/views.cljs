(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views
  (:require
   [re-frame.core :refer [subscribe]]
   [com.yetanalytics.re-route :as re-route]
   [com.yetanalytics.lrs-admin-ui.views.header :refer [header]]
   [com.yetanalytics.lrs-admin-ui.views.main :refer [main]]
   [com.yetanalytics.lrs-admin-ui.views.footer :refer [footer]]
   [com.yetanalytics.lrs-admin-ui.views.login :refer [login]]
   [com.yetanalytics.lrs-admin-ui.views.notification :refer [notifications]]
   [com.yetanalytics.lrs-admin-ui.views.dialog :as dialog]))

(defn app []
  (let [token @(subscribe [:session/get-token])
        route @(subscribe [::re-route/route])]
    (cond
      (= route nil)
      nil
      (= token nil)
      [:div
       [notifications]
       [dialog/dialog]
       [login]
       [footer]]
      :else
      [:div
       [notifications]
       [dialog/dialog]
       [header]
       [main]
       [footer]])))
