(ns com.yetanalytics.lrs-admin-ui.views.credentials.tenant
  (:require
   [com.yetanalytics.lrs-admin-ui.views.credentials.api-key :refer [api-key]]
   [re-frame.core :refer [subscribe dispatch]]))

(defn tenant []
  (let [credentials @(subscribe [:db/get-credentials])]
    [:div {:class "tenant-wrapper"}
     [:div {:class "api-keys-table-actions"}
      [:input {:type "button",
               :class "btn-brand-bold",
               :on-click #(dispatch [:credentials/create-credential {:scopes ["all"]}])
               :value "ADD NEW CREDENTIALS"}]]
     [:div {:class "credential-details"}
      [:div {:class "tenant-count-wrapper"}
       [:span {:class "fg-secondary"} "Number of Credentials: "]
       [:span (count credentials)]]]
     [:div {:class "api-keys-table-header"} "API Key"]
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
               :value "ADD NEW CREDENTIALS"}]]]))
