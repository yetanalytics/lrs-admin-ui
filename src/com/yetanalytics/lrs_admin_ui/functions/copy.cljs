(ns com.yetanalytics.lrs-admin-ui.functions.copy
  (:require [reagent.core :as r]
            ["react-copy-to-clipboard" :refer [CopyToClipboard]]))

(defn copy-text [{:keys [text on-copy]} element]
  [(r/adapt-react-class CopyToClipboard)
   {:text text
    :on-copy on-copy}
   element])
