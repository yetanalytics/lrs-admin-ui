(ns com.yetanalytics.lrs-admin-ui.views.util.table
  (:require
   [reagent.core :as r]
   ["react-data-table-component" :as DataTable]
   [clojure.pprint :refer [pprint]]))

(defn data-table
  [{:keys [columns expandableRowsComponent] :as opts}]
  (let [;; JS INTEROP
        ;; This one is confusing, the input to the column data selector is 
        ;; going to be JSON, so the fn provided won't work. First convert the
        ;; row to EDN then run the selector provided
        cols' (mapv (fn [{:keys [selector] :as col}]
                      (assoc col :selector
                             (fn [x]
                               (-> x
                                   js->clj
                                   selector))))
                    columns)
        ;; Table is expecing a 'React' component, not reagent, so convert the
        ;; reagent component back to react and it will render
        rowComp' (when expandableRowsComponent
                   (r/reactify-component expandableRowsComponent))
        opts' (assoc opts
                     :columns cols'
                     :expandableRowsComponent rowComp')]
    [(r/adapt-react-class (aget DataTable "default")) opts']))
