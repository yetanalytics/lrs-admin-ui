(ns com.yetanalytics.lrs-admin-ui.functions.copy
  (:require [reagent.core :as r]
            ["react-copy-to-clipboard" :refer [CopyToClipboard]]))

(defn copy-text [{:keys [text]} element]
  [(r/adapt-react-class CopyToClipboard)
   {:text text}
   element])
