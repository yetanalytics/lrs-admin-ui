(ns com.yetanalytics.lrs-admin-ui.views.credentials
  (:require
   [com.yetanalytics.lrs-admin-ui.views.credentials.tenant :refer [tenant]]))

(defn credentials []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "Credentials Management"]
   ;;this will be looped for all tenants if tenant mode is enabled (third)
   [tenant]

   #_[:div {:class "border-2 border-dashed my-5"}]
   ])
