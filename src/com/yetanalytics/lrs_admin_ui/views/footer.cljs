(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views.footer
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]))

(defn footer []
  [:footer {:class "home-footer"}
   [:div {:class "bg-primary text-white text-center d-none d-md-block "}
    [:span "@Copyright 2021"]"   |  "
    [:a {:class "text-white", :href "#"} "Terms and Conditions"]"   |  "
    [:a {:class "text-white", :href "#"} "Privacy Policy"]]])
