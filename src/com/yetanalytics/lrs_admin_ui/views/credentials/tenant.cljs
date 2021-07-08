(ns com.yetanalytics.lrs-admin-ui.views.credentials.tenant
  (:require
   [com.yetanalytics.lrs-admin-ui.views.credentials.api-key :refer [api-key]]
   [re-frame.core :refer [subscribe]]
   [clojure.pprint :refer [pprint]]))

(defn tenant []
  [:div {:class "tenant-wrapper"}
   [:div {:class "credential-details"}
    [:div {:class "tenant-title-wrapper"}
     [:span {:class "fg-secondary"} "Tenant: "]
     [:span "Demo"]]
    [:div {:class "tenant-count-wrapper"}
     [:span {:class "fg-secondary"} "Number of Credentials: "]
     [:span "2"]]]
   [:div {:class "api-keys-table-header"} "API Key"]
   [:ol {:class "api-keys-list accordion"}
    ;;will repeat for each key
    [api-key]]
   [:div {:class "api-keys-table-actions"}
    [:input {:type "button", :class "btn-blue-bold", :value "ADD NEW CREDENTIALS"}]]])
