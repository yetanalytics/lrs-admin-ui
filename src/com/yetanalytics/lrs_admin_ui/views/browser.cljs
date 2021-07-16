(ns com.yetanalytics.lrs-admin-ui.views.browser
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [com.yetanalytics.lrs-admin-ui.functions :as fns]
   [com.yetanalytics.lrs-admin-ui.functions.http :as httpfn]
   [clojure.pprint :refer [pprint]]))

(defn process-click
  "Extract the pertinent parts of an element from an event and instrument links
  with appropriate dispatch. ignore non-links and external links"
  [event]
  (let [elem (.-target event)]
    (when (and (= (.-nodeName elem) "A")
               (httpfn/is-rel? (.-href elem)))
      ;;prevent nav
      (fns/ps-event event)
      ;;dispatch xapi parsing and load
      (dispatch [:browser/load-xapi {:path   (.-pathname elem)
                                     :params (.-search elem)}]))))

(defn browser []
  (let [content @(subscribe [:browser/get-content])]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      "Data Browser"]
     [:p (str "Current Query: " @(subscribe [:browser/get-address]))]
     [:div {:class "browser"
            :on-click process-click
            "dangerouslySetInnerHTML" #js{:__html content}}]]))
