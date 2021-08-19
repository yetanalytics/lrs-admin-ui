(ns com.yetanalytics.lrs-admin-ui.views.credentials
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [com.yetanalytics.lrs-admin-ui.views.credentials.tenant :refer [tenant]]))

(defn credentials []
  (dispatch [:credentials/load-credentials])
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "Credentials Management"]
   ;;this will be looped for all tenants if tenant mode is enabled (third)
   [tenant]
   ])
