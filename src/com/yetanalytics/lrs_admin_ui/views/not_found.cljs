(ns com.yetanalytics.lrs-admin-ui.views.not-found
  (:require [re-frame.core :refer [subscribe]]))

(defn not-found []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    @(subscribe [:lang/get :not-found.title])]
   [:p @(subscribe [:lang/get :not-found.body])]])
