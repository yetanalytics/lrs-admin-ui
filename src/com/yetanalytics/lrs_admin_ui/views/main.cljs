(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views.main
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [com.yetanalytics.lrs-admin-ui.views.credentials :refer [credentials]]
   [com.yetanalytics.lrs-admin-ui.views.menu :refer [menu]]))

(defn main []
  [:main {:class "page-creator"}
   [:div {:class "banner-img_box d-none d-md-flex"} ]
   [menu]
   [:div {:class "row no-gutters"}
    [:div {:class "col-xl-8 col-lg-8 col-md-12 col-sm-12 col-12 main-sections"}
     [credentials]]
    [:div {:class "col-xl-4 col-lg-4 col-md-12 col-sm-12 col-12 content-right-wrapper mt-custom"} ]]])
