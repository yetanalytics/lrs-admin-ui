(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views.notification
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [goog.string       :refer [format]]
   goog.string.format))

(defn notification
  [{{:keys [error? msg id]} :notification
    idx :idx}]
  [:div {:class (cond-> "alert-bar"
                  error? (str " error"))
         :style {:top (* 35 idx)}
         :id (str id)}
   [:p {:class "alert"}
    msg
    [:span {:class "close-alert pointer"
            :on-click #(dispatch [:notification/hide id])}
     [:img {:class "close-alert"
            :src (format "/images/icons/icon-close-%s.svg"
                         (cond
                           error? "black"
                           :else "white"))}]]]])

(defn notifications []
  (let [notifications @(subscribe [:notifications/get-notifications])]
    [:div
     (map-indexed
      (fn [idx item]
        [notification {:notification item
                       :key (str "notification-"idx)
                       :idx idx}])
      notifications)]))
