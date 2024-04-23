(ns com.yetanalytics.lrs-admin-ui.views.util.table
  (:require
   [reagent.core :as r]
   ["react-data-table-component" :as DataTable]
   [clojure.pprint :refer [pprint]]))

(defn data-table
  [{:keys [columns expandableRowsComponent data] :as opts}]
  (let [;; JS INTEROP

        ;; This one is confusing, the input to the column data selector is 
        ;; going to be JSON, so the clj fn provided by the calling context 
        ;; won't work. First convert the row to EDN then run the fn provided
        cols' (mapv (fn [{:keys [selector cell] :as col}]
                      (pprint ["conversion check" (fn? selector) (fn? cell)])
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
                        :columns (clj->js cols')
                        :data    (clj->js data)
                        :expandableRowsComponent rowComp')]
    [(r/adapt-react-class (aget DataTable "default")) opts']))
