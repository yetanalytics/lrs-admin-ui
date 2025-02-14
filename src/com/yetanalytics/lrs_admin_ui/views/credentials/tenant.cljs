(ns com.yetanalytics.lrs-admin-ui.views.credentials.tenant
  (:require
   [com.yetanalytics.lrs-admin-ui.views.credentials.api-key :refer [api-key]]
   [re-frame.core :refer [subscribe dispatch]]))

(defn tenant []
  (let [credentials @(subscribe [:db/get-credentials])]
    ;; FIXME: Apply notify-on-seed effect properly after re-route PR is merged
    (when (not-empty credentials)
      (dispatch [:credentials/notify-on-seed]))
    [:div {:class "tenant-wrapper"}
     [:div {:class "api-keys-table-actions"}
      [:input {:type "button",
               :class "btn-brand-bold",
               :on-click #(dispatch [:credentials/create-credential {:scopes ["all"]}])
               :value @(subscribe [:lang/get :credentials.tenant.add])}]]
     [:div {:class "credential-details"}
      [:div {:class "tenant-count-wrapper"}
       [:span {:class "fg-secondary"} @(subscribe [:lang/get :credentials.tenant.number])]
       [:span (count credentials)]]]
     [:div {:class "api-keys-table-header"} @(subscribe [:lang/get :credentials.tenant.key])]
     [:ol {:class "api-keys-list accordion"}
      ;;will repeat for each key
      (map-indexed
       (fn [idx _]
         [api-key {:idx idx :key (str "api-key-" idx)}])
       credentials)]
     [:div {:class "api-keys-table-actions"}
      [:input {:type "button",
               :class "btn-brand-bold",
               :on-click #(dispatch [:credentials/create-credential {:scopes ["all"]}])
               :value @(subscribe [:lang/get :credentials.tenant.add])}]]]))
