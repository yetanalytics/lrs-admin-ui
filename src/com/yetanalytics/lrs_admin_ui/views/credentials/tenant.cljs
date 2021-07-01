(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views.credentials.tenant
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [com.yetanalytics.lrs-admin-ui.views.credentials.api-key :refer [api-key]]))

(defn tenant []
  [:div {:class "general-section-wrapper mt-4"}
   [:div {:class "row no-gutters credential-details mb-1"}
    [:div {:class "col-4"}
     [:span {:class "fg-secondary"} "Tenant: "]
     [:span "Demo"]]
    [:div {:class "col-8 text-right text-lg-left"}
     [:span {:class "fg-secondary"} "Number of Credentials: "]
     [:span "2"]]]
   [:div {:class "general-section-header bg-primary fg-white py-2 px-3 px-lg-5 mb-2 font-condensed-bold"} "API Key"]
   [:ol {:class "accordion mb-2"}
    [api-key]]
   [:div {:class "form-group mt-3 text-right"}
    [:input {:type "button", :class "btn-custom btn-blue font-condensed-bold", :value "ADD NEW CREDENTIALS"}]]])
