(ns com.yetanalytics.lrs-admin-ui.views.update-password
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.copy :refer [copy-text]]
            [com.yetanalytics.lrs-admin-ui.functions.password :as pass]
            [com.yetanalytics.lrs-admin-ui.input :refer [p-min-len]]
            [goog.string :refer [format]]
            goog.string.format))

(defn form []
  (dispatch [:update-password/clear])
  (let [hide-pass (r/atom true)]
    (fn []
      (let [new-password @(subscribe [:update-password/new-password])]
        [:div {:class "update-password-inputs"}
         [:div {:class "row"}
          [:label {:for "old-password-input"} "Old Password:"]
          [:input {:value @(subscribe [:update-password/old-password])
                   :class "new-password round"
                   :id "old-password-input"
                   :type "password"
                   :on-change #(dispatch [:update-password/set-old-password
                                          (fns/ps-event-val %)])}]]
         [:div {:class "row pt-2"}
          [:label {:for "new-password-input"} "New Password:"]
          [:input (cond-> {:value new-password
                           :class "new-password round"
                           :id "new-password-input"
                           :on-change #(dispatch [:update-password/set-new-password
                                                  (fns/ps-event-val %)])}
                    @hide-pass (merge {:type "password"}))]
          [:ul {:class "action-icon-list"}
           [:li
            [:a {:href "#!",
                 :class "icon-secret pointer"
                 :on-click #(swap! hide-pass not)}
             (str (cond
                    @hide-pass "Show"
                    :else "Hide"))]]
           [:li
            [copy-text
             {:text new-password
              :on-copy #(dispatch [:notification/notify false
                                   "Copied New Password!"])}
             [:a {:class "icon-copy pointer"} "Copy"]]]]]
         [:span {:class "password-note"}
          (format "New password must be different from old password and be %d or more characters and contain uppercase, lowercase, numbers, and special characters (%s). Be sure to note or copy the new password as it will not be accessible after creation."
                  p-min-len
                  (apply str pass/special-chars))]
         [:div {:class "row"}
          [:ul {:class "action-icon-list"}
           [:li
            [:a {:href "#!",
                 :on-click #(dispatch [:update-password/generate-password])
                 :class "icon-gen"} [:i "Generate Password"]]]]]]))))

(defn update-password []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "Update Password"]
   [form]
   [:div {:class "update-password-actions"}
    [:input {:type "button",
             :class "btn-brand-bold",
             :on-click #(dispatch [:session/set-page :accounts])
             :value "CANCEL"}]
    [:input {:type "button",
             :class "btn-brand-bold",
             :on-click #(dispatch [:update-password/update-password!])
             :value "UPDATE"}]]])
