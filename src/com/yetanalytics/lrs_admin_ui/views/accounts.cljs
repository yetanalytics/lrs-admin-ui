(ns com.yetanalytics.lrs-admin-ui.views.accounts
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [com.yetanalytics.lrs-admin-ui.functions :as fns]))



(defn account [{:keys [username account-id]}]
  [:li {:class "mb-2"}
   [:div {:class "accordion-container"}
    [:div {:class "account-row"}
     [:div {:class "account-col"}
      [:p username]]
     [:div {:class "account-col"}
      [:ul {:class "action-icon-list"}
       [:li
        [:a {:href "#!",
             :on-click #(dispatch [:accounts/delete-account account-id])
             :class "icon-delete"} "Delete"]]]]]]])

(defn new-account []
  (let [new-account @(subscribe [:db/get-new-account])]
    [:div {:class "create-account-inputs"}
     [:div {:class "row"}
      [:label {:for "new-username-input"} "Username:"]
      [:input {:value (:username new-account)
               :class "new-account"
               :id "new-username-input"
               :on-change #(dispatch [:new-account/set-username (fns/ps-event-val %)])}]]
     [:div {:class "row"}
      [:label {:for "new-password-input"} "Password:"]
      [:input {:value (:password new-account)
               :class "new-account"
               :id "new-password-input"
               :on-change #(dispatch [:new-account/set-password (fns/ps-event-val %)])}]]
     [:div {:class "row"}
      [:ul {:class "action-icon-list"}
       [:li
        [:a {:href "#!",
             :on-click #(dispatch [:new-account/generate-password])
             :class "icon-gen"} [:i "Generate Password"]]]]]]))

(defn accounts []
  (dispatch [:accounts/load-accounts])
  (let [accounts @(subscribe [:db/get-accounts])]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      "Account Management"]
     ;;this will be looped for all tenants if tenant mode is enabled (third)
     [:div {:class "tenant-wrapper"}
      [:div {:class "accounts-table-header"} "Account"]
      [:ol {:class "accounts-list accordion"}
       ;;will repeat for each key
       (map-indexed
        (fn [idx acct]
          [account acct])
        accounts)]
      [:div {:class "h-divider"}]
      [:h3 {:class "content-title"} "Create New Account"]
      [new-account]
      [:div {:class "accounts-table-actions"}
       [:input {:type "button",
                :class "btn-blue-bold",
                :on-click #(dispatch [:accounts/create-account])
                :value "CREATE ACCOUNT"}]]]]))
