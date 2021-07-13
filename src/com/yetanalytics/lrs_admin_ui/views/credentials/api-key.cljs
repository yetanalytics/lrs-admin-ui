(ns com.yetanalytics.lrs-admin-ui.views.credentials.api-key
  (:require
   [reagent.core :as r]))


(defn has-scope
  [list scope]
  (some? (some #{scope} list)))

(defn toggle-scope
  [scope list]
  (println list)
  (println scope)
  (if (has-scope list scope)
    (remove #(= scope %) list)
    (conj list scope)))

(defn api-key
  [{:keys [credential]}]
  (let [expanded (r/atom false)
        show-secret (r/atom false)
        edit (r/atom false)]
    (fn []
      [:li {:class "mb-2"}
       [:div {:class "accordion-container"}
        [:div {:class "api-key-row"
               :aria-label "Show/Hide Api Key Details"
               :on-click #(swap! expanded not)}
         [:div {:class "api-key-col"}
          [:span {:class (str "collapse-sign"
                              (when @expanded " expanded"))}
           (:api-key credential)]]
         [:div {:class "api-key-col"} "Permissions: "
          (map (fn [scope]
                 [:span scope])
               (:scopes credential))]]
        (when @expanded
          [:div {:class "api-key-expand"}
           [:div {:class "api-key-col"}
            [:p {:class "api-key-col-header"}
             "API Key Secret"]
            [:div {:class "action-row"}
             [:div {:class "action-label-wide"}
              (cond
                @show-secret [:span (:secret-key credential)]
                :else "Redacted")]
             [:ul {:class "action-icon-list"}
              [:li
               [:a {:href "#!",
                    :class "icon-secret"
                    :on-click #(swap! show-secret not)}
                (str (cond
                       @show-secret "Hide"
                       :else "Show")
                     " Secret")]]]]]
           [:div {:class "api-key-col"}
            [:p {:class "api-key-col-header"}
             "Permissions"]
            (cond
              @edit
              (let [scopes (r/atom (:scopes credential))]
                (println "current scopes")
                (println scopes)
                [:div {:class "action-row"}
                 [:ul {:class "role-select"}
                  (map (fn [scope]
                         [:li {:class "role-checkbox"}
                          [:input {:type "checkbox",
                                   :name "scopes",
                                   :value scope
                                   :checked (has-scope @scopes scope)
                                   :on-change (fn [e]
                                                (swap! scopes
                                                       (partial toggle-scope scope)))}]
                          [:label {:for "scopes"} (str " " scope)]]
                         )
                       ["statements/write"
                        "statements/read"
                        "all/read"
                        "all"])]
                 [:ul {:class "action-icon-list"}
                  [:li
                   [:a {:href "#!", :class "icon-save"} "Save"]]
                  [:li
                   [:a {:href "#!",
                        :on-click #(swap! edit not)
                        :class "icon-close"} "Cancel"]]]])
              :else
              [:div {:class "action-row"}
               [:div {:class "action-label"}
                "All"]
               [:ul {:class "action-icon-list"}
                [:li
                 [:a {:href "#!",
                      :on-click #(swap! edit not)
                      :class "icon-edit"} "Edit"]]
                [:li
                 [:a {:href "#!", :class "icon-delete"} "Delete"]]]])]])]])))
