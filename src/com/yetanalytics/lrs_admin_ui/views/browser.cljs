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
   [com.yetanalytics.lrs-admin-ui.views.util.langmap :refer [langmap]]))

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

(defn filter-button
  [{:keys [key value title]}]
  [:a {:class "pointer"
       :title title
       :on-click #(dispatch [:browser/add-filter key value])}
   [:img {:src "images/icons/add-filter.svg"
          :width "15px"
          :height "20px"}]])

(defn verb-cell
  [row]
  (let [{:strs [verb]} (js->clj row)
        {:strs [id]} verb]
    (r/as-element
     [:div.custom-cell
      [:div.cell-display
       (verb-display verb)]
      [:div.cell-action 
       [filter-button {:title "Filter by Verb"
                       :key   :verb
                       :value id}]]])))

(defn actor-cell
  [row]
  (let [{:strs [actor]} (js->clj row)]
    (r/as-element
     [:div.custom-cell
      [:div.cell-display
       (actor-display actor)]
      [:div.cell-action
       [filter-button {:title "Filter by Actor"
                       :key   :agent
                       :value (js/JSON.stringify (clj->js actor))}]]])))

(defn object-cell 
  [row]
  (let [{:strs [object]} (js->clj row)
        {:strs [id]} object]
    (r/as-element
     [:div.custom-cell
      [:div.cell-display
       (object-display object)]
      [:div.cell-action
       (when id
         [filter-button {:title "Filter by Activity"
                         :key   :activity
                         :value id}])]])))

(defn row-num-cell
  [_ idx]
  (let [page-start 
        (+ 1 (* @(subscribe [:browser/get-batch-size])
                (count @(subscribe [:browser/get-back-stack]))))]
    (r/as-element [:i (+ idx page-start)])))

(defn view-statement-json
  [row]
  (json-viewer
   {:data (:data row)
    ;; top level expanded only
    :collapsed 1}))

(defn statement-table 
  [{:keys [data]}]
  (let [cols [{:name (r/as-element [:i "#"])
               :cell row-num-cell
               :maxWidth "10px"}
              {:name "Statement ID"
               :selector #(get % "id")
               ;; ensure id is readable
               :minWidth "300px"}
              {:name "Actor"
               :cell actor-cell}
              {:name "Verb"
               :cell verb-cell}
              {:name "Object"
               :cell object-cell}
              {:name "Timestamp"
               :selector #(time/iso8601->local-display (get % "timestamp"))}]
        opts {:columns            cols
              :data               data
              :expandableRows     true
              :expandOnRowClicked true
              :dense              false
              :expandableRowsComponent view-statement-json}
        b-s  @(subscribe [:browser/get-back-stack])] 
    [:div 
     [data-table opts]
     [:div {:class "table-nav"}
      [:div {:class "table-nav-back"}
       (when (seq b-s)
         [:a {:on-click #(dispatch [:browser/back])
              :class "pointer"}
          [:img {:src "images/icons/prev.svg"
                 :width "30px"}]])] 
      [:div {:class "table-nav-pages"}
       [:span " Page: " (+ 1 (count b-s))]]
      [:div {:class "table-nav-rows"}
       [:span " Rows Per Page: "]
       [:select
        {:name "batch_size"
         :on-change 
         #(dispatch [:browser/update-batch-size (int (fns/ps-event-val %))])
         :value @(subscribe [:browser/get-batch-size])}
        [:option {:value "10"} "10"]
        [:option {:value "20"} "20"]
        [:option {:value "50"} "50"]
        [:option {:value "100"} "100"]]]
      [:div {:class "table-nav-next"}
       (when (seq @(subscribe [:browser/get-more-link]))
         [:a {:on-click #(dispatch [:browser/more])
              :class "pointer"}
          [:img {:src "images/icons/next.svg"
                 :width "30px"}]])]]]))

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
