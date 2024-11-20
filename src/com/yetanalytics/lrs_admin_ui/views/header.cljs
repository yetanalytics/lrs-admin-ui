(ns com.yetanalytics.lrs-admin-ui.views.header
  (:require [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]))

(defn username []
  (let [display-name @(subscribe [:session/get-display-name])]
    [:div {:class "user-name"}
     [:span @(subscribe [:lang/get :header.welcome])]
     (if @(subscribe [:oidc/enabled?])
       [:span display-name]
       [:a {:class "fg-primary"
            :href "#"
            :on-click (fn [e]
                        (fns/ps-event e)
                        (dispatch [:session/set-page :update-password]))}
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
                       (dispatch-sync [:session/logout]))}
       @(subscribe [:lang/get :header.logout])]]]]])
