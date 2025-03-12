(ns com.yetanalytics.lrs-admin-ui.views.browser.download-csv
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.views.reactions.path :as p]))

(defn- path-edit
  [idx path & {:keys [remove-fn
                      validate?
                      open-next?]
               :or   {validate?  true
                      open-next? false}}]
  [p/path-input path
   :add-fn    (fn []
                (dispatch [:csv/add-property-path-segment idx]))
   :del-fn    (fn []
                (dispatch [:csv/delete-property-path-segment idx]))
   :change-fn (fn [seg-val]
                (dispatch [:csv/change-property-path-segment idx seg-val open-next?]))
   :remove-fn remove-fn
   :validate? validate?])

(defn property-paths []
  (let [paths-expand (r/atom false)]
    (fn []
      (let [property-paths @(subscribe [:csv/property-paths])]
        [:div {:class "filters-wrapper"}
         [:div {:class (str "pointer collapse-sign"
                            (when @paths-expand " expanded"))
                :on-click #(swap! paths-expand not)}
          @(subscribe [:lang/get :csv-property-paths.title])]
         (when @paths-expand
           [:<>
            (into
             [:ul {:class "identity-paths"}]
             (map-indexed
              (fn [idx path]
                [:li [path-edit idx path
                      :remove-fn #(dispatch [:csv/delete-property-path idx])
                      :open-next? true]])
              property-paths))
            [:span.add-identity-path
             [:a {:href "#"
                  :on-click (fn [e]
                              (fns/ps-event e)
                              (dispatch [:csv/add-property-path]))}
              @(subscribe [:lang/get :csv.property-paths.add])
              [:img {:src @(subscribe [:resources/icon "add.svg"])}]]]])]))))
