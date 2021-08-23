(ns com.yetanalytics.lrs-admin-ui.views.menu
  (:require
   [re-frame.core :refer [subscribe dispatch-sync]]))

(defn menu-item
  [{:keys [name page]}]
  [:li {:class "banner-link-item"}
   [:a (cond-> {:href "#"
                :on-click #(dispatch-sync [:session/set-page page])}
         (= page @(subscribe [:session/get-page])) (merge {:class "active"}))
    name]])

(defn menu []
  [:div {:class "banner-link-box"}
   [:ul
    [menu-item {:name "Credentials Management" :page :credentials}]
    [menu-item {:name "Account Management" :page :accounts}]
    [menu-item {:name "Statement Browser" :page :browser}]]])
