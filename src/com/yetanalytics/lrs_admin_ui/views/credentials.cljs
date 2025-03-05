(ns com.yetanalytics.lrs-admin-ui.views.credentials
  (:require
   [re-frame.core :refer [subscribe]]
   [com.yetanalytics.lrs-admin-ui.views.credentials.tenant :refer [tenant]]))

(defn credentials []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    @(subscribe [:lang/get :credentials.title])]
   ;; this will be looped for all tenants if tenant mode is enabled (third)
   [tenant]])
