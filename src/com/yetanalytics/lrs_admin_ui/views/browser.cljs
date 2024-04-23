(ns com.yetanalytics.lrs-admin-ui.views.browser
  (:require
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [clojure.string :as cstr]
   [com.yetanalytics.lrs-admin-ui.functions :as fns]
   [com.yetanalytics.lrs-admin-ui.functions.http :as httpfn]
   [com.yetanalytics.lrs-admin-ui.functions.scopes :as scopes]
   [com.yetanalytics.lrs-admin-ui.functions.time :as time]
   [com.yetanalytics.lrs-admin-ui.views.util.json :refer [json-viewer]]
   [com.yetanalytics.lrs-admin-ui.views.util.table :refer [data-table]]
   [com.yetanalytics.lrs-admin-ui.views.util.langmap :refer [langmap]]
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

(defn actor-display 
  "Actor IFI progressive resolution to a display string."
  [actor]
  (or (get actor "name")
      (get-in actor ["account" "name"])
      (get actor "mbox")
      (get actor "mbox_sha1sum")
      (get actor "openid")))

(defn object-display
  "Object display resolution including the possibility of actor object"
  [object]
  (or (langmap (get-in object ["definition" "name"]))
      (get object "id")
      (actor-display object)))

(defn verb-display
  "Verb display resolution"
  [verb]
  (or (langmap (get verb "display"))
      (get verb "id")))

(defn view-statement-json
  [row]
  (json-viewer
   {:data (:data row)
    :collapsed 1}))

(defn statement-table 
  [{:keys [data]}]
  (let [cols [{:name "Statement ID"
               :selector #(get % "id")
               :minWidth "300px"}
              {:name "Actor"
               :selector #(actor-display (get % "actor"))}
              {:name "Verb"
               :selector #(verb-display (get % "verb"))}
              {:name "Object"
               :selector #(object-display (get % "object"))}
              {:name "Timestamp"
               :selector #(time/iso8601->local-display (get % "timestamp"))}]
        opts {:columns        cols
              :data           data
              :expandableRows true
              :expandableRowsComponent 
              view-statement-json}] 
    [:div 
     [data-table opts]
     [:p 
      (when (seq @(subscribe [:browser/get-back-stack]))
        [:a {:on-click #(dispatch [:browser/back])
             :class "pointer"} "Back"])
      [:select
       {:name "batch_size"
        :on-change 
        #(dispatch [:browser/update-batch-size (int (fns/ps-event-val %))])
        :value @(subscribe [:browser/get-batch-size])}
       [:option {:value "10"} "10"]
       [:option {:value "20"} "20"]
       [:option {:value "50"} "50"]
       [:option {:value "100"} "100"]]
      (when (seq @(subscribe [:browser/get-more-link]))
        [:a {:on-click #(dispatch [:browser/more])
             :class "pointer"} "Next"])]]))

(defn browser []
  (let [filter-expand (r/atom false)]
    (fn []
      (let [content @(subscribe [:browser/get-content])
            ;;filter out credentials that can't read the LRS
            read-credentials (filter scopes/has-statement-read-scopes?
                                     @(subscribe [:db/get-credentials]))]
        [:div {:class "left-content-wrapper"}
         [:h2 {:class "content-title"}
          @(subscribe [:lang/get :browser.title])]
         [:p
          [:span
           [:b @(subscribe [:lang/get :browser.credentials])]
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
         (let [address @(subscribe [:browser/get-address])
               params  (httpfn/extract-params address)]
           (when (some? address)
             [:div {:class "browser-filters"}
              [:p @(subscribe [:lang/get :browser.query])]
              [:div {:class "xapi-address"}
               address]
              (when (not-empty params)
                [:div {:class "filters-wrapper"}
                 [:span {:class (str "pointer collapse-sign"
                                     (when @filter-expand " expanded"))
                         :on-click #(swap! filter-expand not)}
                  "Filters:"]
                 (when @filter-expand
                   [:div
                    [:div {:class "filter-table"}
                     (map
                      (fn [[key val]]
                        [:div {:class "filter-row"}
                         [:div {:class "filter-column key"}
                          key]
                         [:div {:class "filter-column"}
                          val]])
                      (seq params))]
                    [:ul {:class "action-icon-list"}
                     [:li
                      [:a {:href "#!"
                           :on-click #(dispatch [:browser/load-xapi])
                           :class "icon-clear-filters"} "Clear Filters"]]]])])]))
         (if (cstr/blank? content)
           [:div {:class "browser"}
            @(subscribe [:lang/get :browser.key-note])]
           [statement-table {:data content}])]))))
