(ns com.yetanalytics.lrs-admin-ui.views.users
  (:require
   [re-frame.core :refer [subscribe dispatch]]))



(defn user []
  )


(defn users []
  (dispatch [:users/load-users])
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "User Management"]
   ;;this will be looped for all tenants if tenant mode is enabled (third)
   [:div {:class "tenant-wrapper"}
    [:div {:class "api-keys-table-header"} "User"]
    [:ol {:class "api-keys-list accordion"}
     ;;will repeat for each key
     (map-indexed
      (fn [idx credential]
        [api-key {:idx idx :key (str "api-key-" idx)}])
      credentials)]
    [:div {:class "api-keys-table-actions"}
     [:input {:type "button",
              :class "btn-blue-bold",
              :on-click #(dispatch [:credentials/create-credential {:scopes ["all"]}])
              :value "ADD NEW CREDENTIALS"}]]]
   ])
