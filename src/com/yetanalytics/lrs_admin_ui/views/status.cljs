(ns com.yetanalytics.lrs-admin-ui.views.status
  (:require
   [re-frame.core :refer [subscribe dispatch]]))

(defn bignum
  [label sub-qvec]
  [:div.bignum
   [:div.bignum-label
    label]
   [:div.bignum-number
    @(subscribe sub-qvec)]])

(defn refresh-button
  []
  [:input.btn-blue-bold
   {:type "button"
    :value "REFRESH"
    :on-click #(dispatch [:status/get-data])}])

(defn status
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "LRS Status"]
   [refresh-button]
   [bignum "Statement Count" [:status.data/statement-count]]])
