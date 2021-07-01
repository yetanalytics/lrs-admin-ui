(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views.header
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]))

(defn header []
  [:header {:class "container-fluid"}
   [:div {:class "d-flex align-items-center justify-content-between"}
    [:div {:class "post-image"}
     [:i
      [:img {:src "/images/logo.png", :alt "logo", :class "logo-img"}]]]
    [:div {:class "text-right"}
     [:div {:class "text-white user-name"} "Welcome Mike"]
     [:div {:class "d-md-block d-none fg-primary"}
      [:a {:class "fg-primary", :href "#"} "Logout"]]]]])
