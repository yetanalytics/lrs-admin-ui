(ns com.yetanalytics.lrs-admin-ui.views.main
  (:require
   [re-frame.core :refer [subscribe]]
   [com.yetanalytics.lrs-admin-ui.views.credentials :refer [credentials]]
   [com.yetanalytics.lrs-admin-ui.views.browser :refer [browser]]
   [com.yetanalytics.lrs-admin-ui.views.menu :refer [menu]]))

(defn main []
  [:main {:class "lrs-main"}
   [:div {:class "banner-img_box"}]
   [menu]
   [:div {:class "row no-gutters"}
    [:div {:class "main-sections"}
     (case @(subscribe [:session/get-page])
       :credentials [credentials]
       :browser [browser])]
    [:div {:class "content-right-wrapper"} ]]])
