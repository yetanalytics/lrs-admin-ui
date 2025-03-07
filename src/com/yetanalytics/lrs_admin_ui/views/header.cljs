(ns com.yetanalytics.lrs-admin-ui.views.header
  (:require [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.re-route :as re-route]
            [re-frame.core :refer [subscribe dispatch-sync]]))

(defn username []
  (let [display-name @(subscribe [:session/get-display-name])]
    [:div {:class "user-name"}
     [:span @(subscribe [:lang/get :header.welcome])]
     (if @(subscribe [:oidc/enabled?])
       [:span display-name]
       [:a {:class "fg-primary"
            :href @(subscribe [::re-route/href :update-password])}
        display-name])]))

(defn header []
  [:header {:class "container-fluid"}
   [:div {:class "header-wrapper"}
    [:div {:class "post-image"}
     [:i
      [:img {:src   @(subscribe [:resources/image "logo.png"])
             :alt   "logo"
             :class "logo-img"}]]]
    [:div {:class "text-right"}
     [username]
     [:div {:class "header-actions-wrapper"}
      [:a {:class "fg-primary",
           :href "#"
           :on-click (fn [e]
                       (fns/ps-event e)
                       (dispatch-sync [:logout/logout]))}
       @(subscribe [:lang/get :header.logout])]]]]])
