(ns com.yetanalytics.lrs-admin-ui.views.browser.download-csv
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.views.reactions.path :as p]))

(defn- path-edit
  [{:keys [index path up-disabled? down-disabled?]}]
  [:div {:class "property-path"}
   [:img {:class    (str "orderer" (when up-disabled? " disabled"))
          :src      @(subscribe [:resources/icon "icon-arrow-up-blue.svg"])
          :on-click (fn []
                      (dispatch [:csv/move-property-path-up index]))}]
   [:img {:class    (str "orderer" (when down-disabled? " disabled"))
          :src      @(subscribe [:resources/icon "icon-arrow-down-blue.svg"])
          :on-click (fn []
                      (dispatch [:csv/move-property-path-down index]))}]
   [p/path-input path
    :add-fn    (fn []
                 (dispatch [:csv/add-property-path-segment index]))
    :del-fn    (fn []
                 (dispatch [:csv/delete-property-path-segment index]))
    :change-fn (fn [seg-val]
                 (dispatch [:csv/change-property-path-segment index seg-val true]))
    :remove-fn (fn []
                 (dispatch [:csv/delete-property-path index]))
    :validate? true]])

(defn property-paths []
  (let [paths-expand (r/atom false)]
    (fn []
      (let [property-paths @(subscribe [:csv/property-paths])
            max-index      (dec (count property-paths))]
        [:div {:class "filters-wrapper"}
         [:div {:class (str "pointer collapse-sign"
                            (when @paths-expand " expanded"))
                :on-click #(swap! paths-expand not)}
          @(subscribe [:lang/get :csv.property-paths.title])]
         (when @paths-expand
           [:div
            [:p [:em @(subscribe [:lang/get :csv.property-paths.instructions])]]
            (into
             [:ul {:class "identity-paths"}]
             (map-indexed
              (fn [idx path]
                [:li [path-edit {:index          idx
                                 :path           path
                                 :up-disabled?   (<= idx 0)
                                 :down-disabled? (>= idx max-index)}]])
              property-paths))
            [:span.add-identity-path
             [:a {:href "#"
                  :on-click (fn [e]
                              (fns/ps-event e)
                              (dispatch [:csv/add-property-path]))}
              @(subscribe [:lang/get :csv.property-paths.add])
              [:img {:src @(subscribe [:resources/icon "add.svg"])}]]]])]))))
