(ns com.yetanalytics.lrs-admin-ui.views.header
  (:require [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [re-frame.core :refer [subscribe dispatch-sync]]
            [goog.string       :refer [format]]
            goog.string.format))

(defn header []
  [:header {:class "container-fluid"}
   [:div {:class "header-wrapper"}
    [:div {:class "post-image"}
     [:i
      [:img {:src "/images/logo.png", :alt "logo", :class "logo-img"}]]]
    [:div {:class "text-right"}
     [:div {:class "user-name"} (format "Welcome, %s" @(subscribe [:session/get-username]))]
     [:div {:class "header-actions-wrapper"}
      [:a {:class "fg-primary",
           :href "#"
           :on-click (fn [e]
                       (fns/ps-event e)
                       (dispatch-sync [:session/set-token nil]))}
       "Logout"]]]]])
