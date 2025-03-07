(ns com.yetanalytics.lrs-admin-ui.views.update-password
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [com.yetanalytics.re-route :as re-route]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.copy :refer [copy-text]]
            [com.yetanalytics.lrs-admin-ui.functions.password :as pass]
            [com.yetanalytics.lrs-admin-ui.input :refer [p-min-len]]
            [goog.string :refer [format]]
            goog.string.format))

(defn form []
  (let [hide-pass (r/atom true)]
    (fn []
      (let [new-password @(subscribe [:update-password/new-password])]
        [:div {:class "update-password-inputs"}
         [:div {:class "row"}
          [:div 
           [:label {:for "old-password-input"} 
            @(subscribe [:lang/get :account-mgmt.update-password.password.old])]
           [:input {:value @(subscribe [:update-password/old-password])
                    :class "new-password round"
                    :id "old-password-input"
                    :type "password"
                    :on-change #(dispatch [:update-password/set-old-password
                                           (fns/ps-event-val %)])}]]]
         [:div {:class "row pt-2"}
          [:div
           [:label {:for "new-password-input"}
            @(subscribe [:lang/get :account-mgmt.update-password.password.new])]
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
                     @hide-pass @(subscribe [:lang/get :account-mgmt.update-password.password.show])
                     :else @(subscribe [:lang/get :account-mgmt.update-password.password.hide])))]]
            [:li
             [copy-text
              {:text new-password
               :on-copy #(dispatch [:notification/notify false
                                    @(subscribe 
                                      [:lang/get
                                       :notification.account-mgmt.copied-password])])}
              [:a {:class "icon-copy pointer"} @(subscribe [:lang/get :account-mgmt.update-password.password.copy])]]]]]]
         [:span {:class "password-note"}
          (format @(subscribe [:lang/get :account-mgmt.update-password.guidelines])
                  p-min-len
                  (apply str pass/special-chars))]
         [:div {:class "row"}
          [:ul {:class "action-icon-list"}
           [:li
            [:a {:href "#!",
                 :on-click #(dispatch [:update-password/generate-password])
                 :class "icon-gen"} [:i @(subscribe [:lang/get :account-mgmt.update-password.password.generate])]]]]]]))))

(defn update-password []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    @(subscribe [:lang/get :account-mgmt.update-password.title])]
   [form]
   [:div {:class "update-password-actions"}
    [:input {:type "button",
             :class "btn-brand-bold",
             :on-click #(dispatch [::re-route/navigate :accounts])
             :value @(subscribe [:lang/get :account-mgmt.update-password.cancel])}]
    [:input {:type "button",
             :class "btn-brand-bold",
             :on-click #(dispatch [:update-password/update-password!])
             :value @(subscribe [:lang/get :account-mgmt.update-password.update])}]]])
