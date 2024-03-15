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
    [menu-item {:name @(subscribe [:lang/get :header.nav.credentials]) :page :credentials}]
    (when @(subscribe [:oidc/show-account-nav?])
      [menu-item {:name @(subscribe [:lang/get :header.nav.accounts]) :page :accounts}])
    (when @(subscribe [:db/get-stmt-html-enabled])
      [menu-item {:name @(subscribe [:lang/get :header.nav.browser]) :page :browser}])
    (when @(subscribe [:status/enabled?])
      [menu-item {:name @(subscribe [:lang/get :header.nav.monitor]) :page :status}])
    (when (some identity [@(subscribe [:delete-actor/enabled?])])
      [menu-item {:name @(subscribe [:lang/get :header.nav.data]) :page :data-management}])
    (when @(subscribe [:reaction/enabled?])
      [menu-item {:name @(subscribe [:lang/get :header.nav.reactions]) :page :reactions}])]])
