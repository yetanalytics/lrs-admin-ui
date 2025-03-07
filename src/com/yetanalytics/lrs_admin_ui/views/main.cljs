(ns com.yetanalytics.lrs-admin-ui.views.main
  (:require
   [re-frame.core :refer [subscribe]]
   [com.yetanalytics.re-route :as re-route]
   [com.yetanalytics.lrs-admin-ui.views.menu :refer [menu]]))

(defn main []
  [:main {:class "lrs-main"}
   [:div {:class "banner-img_box"}]
   [menu]
   [:div {:class "row no-gutters"}
    [:div {:class "main-sections"}
     (when-some [page @(subscribe [::re-route/route-view])]
       [page])]
    [:div {:class "content-right-wrapper"}]]])
