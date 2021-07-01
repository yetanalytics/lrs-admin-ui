(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views.credentials
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [com.yetanalytics.lrs-admin-ui.views.credentials.tenant :refer [tenant]]))

(defn credentials []
  [:div {:class "container-fluid pb-80"}
   [:h2 {:class "font-rama-semi-bold"} "Credentials Management"]
   ;;this will be looped for all tenants if tenant mode is enabled (third)
   [tenant]

   #_[:div {:class "border-2 border-dashed my-5"}]
   ])
