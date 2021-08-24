(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views.notification
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [goog.string       :refer [format]]
   goog.string.format))

(defn alert-bar []
  (let [{:keys [error? visible? msg]}
        @(subscribe [:notification/get-notification])]
    [:div {:class (cond-> "alert-bar"
                    error? (str " error")
                    visible? (str " visible"))}
     (when visible?
       [:p {:class "alert"}
        msg
        [:span {:class "close-alert pointer"
                :on-click #(dispatch [:notification/hide])}
         [:img {:class "close-alert"
                :src (format "/images/icons/icon-close-%s.svg"
                             (cond
                               error? "black"
                               :else "white"))}]]])]))
