(ns com.yetanalytics.lrs-admin-ui.views.main
  (:require
   [re-frame.core :refer [subscribe]]
   [com.yetanalytics.lrs-admin-ui.views.credentials :refer [credentials]]
   [com.yetanalytics.lrs-admin-ui.views.browser :refer [browser]]
   [com.yetanalytics.lrs-admin-ui.views.menu :refer [menu]]
   [com.yetanalytics.lrs-admin-ui.views.accounts :refer [accounts]]
   [com.yetanalytics.lrs-admin-ui.views.status :refer [status]]
   [com.yetanalytics.lrs-admin-ui.views.update-password :refer [update-password]]
   [com.yetanalytics.lrs-admin-ui.views.special-actions :refer [special-actions]]))

(defn main []
  [:main {:class "lrs-main"}
   [:div {:class "banner-img_box"}]
   [menu]
   [:div {:class "row no-gutters"}
    [:div {:class "main-sections"}
     (case @(subscribe [:session/get-page])
       :credentials [credentials]
       :browser [browser]
       :accounts [accounts]
       :status [status]
       :update-password [update-password]
       :special-actions [special-actions])]
    [:div {:class "content-right-wrapper"}]]])
