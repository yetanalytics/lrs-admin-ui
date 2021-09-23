(ns com.yetanalytics.lrs-admin-ui.views.browser
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [com.yetanalytics.lrs-admin-ui.functions :as fns]
   [com.yetanalytics.lrs-admin-ui.functions.http :as httpfn]
   [clojure.string :refer [blank?]]
   [clojure.pprint :refer [pprint]]))

;; Credential scopes to allow the browser to use (those which can read)
(def read-scopes #{"all" "all/read" "statements/read"})

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
  (let [content @(subscribe [:browser/get-content])
        ;;filter out credentials that can't read the LRS
        read-credentials (filter #(some read-scopes (:scopes %))
                                 @(subscribe [:db/get-credentials]))]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      "Data Browser"]
     [:p
      [:span
       [:b "Credentials to Use: "]
       [:select {:name (str "update_credential")
                 :on-change #(dispatch [:browser/update-credential
                                        (fns/ps-event-val %)])}
        [:option "Credential to Browse"]
        (map-indexed
         (fn [idx credential]
           [:option {:value (:api-key credential)
                     :key (str "browser-credential-" idx)}
            (fns/elide (:api-key credential) 20)])
         read-credentials)]
       ]]
     (let [address @(subscribe [:browser/get-address])]
       (when (some? address)
         [:div
          [:p [:b (str "Current Query: " address)]]
          [:ul {:class "action-icon-list"}
           [:li
            [:a {:href "#!",
                 :on-click #(dispatch [:browser/load-xapi])
                 :class "icon-clear-filters"} "Clear Filters"]]]]))
     (if (blank? content)
       [:div {:class "browser"}
        "Please Choose an API Key Above to Browse LRS Data"]
       [:div {:class "browser"
              :on-click process-click
              "dangerouslySetInnerHTML" #js{:__html content}}])]))
