(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views
  (:require
   [re-frame.core :refer [subscribe]]
   [com.yetanalytics.lrs-admin-ui.views.header :refer [header]]
   [com.yetanalytics.lrs-admin-ui.views.main :refer [main]]
   [com.yetanalytics.lrs-admin-ui.views.footer :refer [footer]]
   [com.yetanalytics.lrs-admin-ui.views.login :refer [login]]
   [com.yetanalytics.lrs-admin-ui.views.notification :refer [notifications]]))

(defn app []
  (let [token @(subscribe [:session/get-token])]
    (cond
      (= token nil)
      [:div
       [notifications]
       [login]
       [footer]]
      :else
      [:div
       [notifications]
       [header]
       [main]
       [footer]])))
