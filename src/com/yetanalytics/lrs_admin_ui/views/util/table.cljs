(ns com.yetanalytics.lrs-admin-ui.views.util.table
  (:require
   [reagent.core :as r]
   ["react-data-table-component" :as DataTable]))

(def default-table-style
  {:header    {:style {:minHeight "56px"}}
   :headRow   {:style {:borderTopStyle "solid"
                       :borderTopWidth "1px"
                       :borderTopColor "lightgray"
                       :fontWeight "bold"}}
   :headCells {:style {"&:not(:last-of-type)" {:borderRightStyle "solid"
                                               :borderRightWidth "1px"
                                               :borderRightColor "lightgray"}}}
   :cells     {:style {"&:not(:last-of-type)" {:borderRightStyle "solid"
                                               :borderRightWidth "1px"
                                               :borderRightColor "lightgray"}}}});

(defn data-table
  [{:keys [columns expandableRowsComponent data customStyles] :as opts
    :or   {customStyles default-table-style}}]
  (let [;; JS INTEROP

        ;; This one is confusing, the input to the column data selector is 
        ;; going to be JSON, so the clj fn provided by the calling context 
        ;; won't work. First convert the row to EDN then run the fn provided
        cols' (mapv (fn [{:keys [selector] :as col}]
                      (cond-> col
                        (fn? selector)
                        (assoc :selector (fn [x] (-> x js->clj selector)))))
                    columns)
        ;; Table is expecing a 'React' component, not reagent, so we convert 
        ;; the reagent component back to react and it will render
        rowComp' (when expandableRowsComponent
                   (r/reactify-component expandableRowsComponent))
        ;; Reassemble options
        opts'    (assoc opts
                        :columns      (clj->js cols')
                        :data         (clj->js data)
                        :customStyles (clj->js customStyles)
                        :expandableRowsComponent rowComp')]
    [(r/adapt-react-class (aget DataTable "default")) opts']))
