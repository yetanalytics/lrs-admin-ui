(ns com.yetanalytics.lrs-admin-ui.views.main
  (:require
   [com.yetanalytics.lrs-admin-ui.views.credentials :refer [credentials]]
   [com.yetanalytics.lrs-admin-ui.views.menu :refer [menu]]))

(defn main []
  [:main {:class "lrs-main"}
   [:div {:class "banner-img_box"} ]
   [menu]
   [:div {:class "row no-gutters"}
    [:div {:class "main-sections"}
     ;;this will swap credentials, statement browser, etc. Might put a light
     ;;router in
     [credentials]]
    [:div {:class "content-right-wrapper"} ]]])
