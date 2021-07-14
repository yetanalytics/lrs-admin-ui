(ns com.yetanalytics.lrs-admin-ui.views.credentials.api-key
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame :refer [dispatch subscribe]]
   [com.yetanalytics.lrs-admin-ui.functions.copy :refer [copy-text]]))

(defn has-scope
  [list scope]
  (some? (some #{scope} list)))

(defn toggle-scope
  [scope list]
  (if (has-scope list scope)
    (remove #(= scope %) list)
    (conj list scope)))

(defn api-key
  [{:keys [idx]}]
  (let [expanded (r/atom false)
        show-secret (r/atom false)
        edit (r/atom false)]
    (fn []
      (let [credential @(subscribe [:credentials/get-credential idx])
            scopes     (:scopes credential)]
        [:li {:class "mb-2"}
         [:div {:class "accordion-container"}
          [:div {:class "api-key-row"
                 :aria-label "Show/Hide Api Key Details"
                 :on-click #(swap! expanded not)}
           [:div {:class "api-key-col"}
            [:span {:class (str "collapse-sign"
                                (when @expanded " expanded"))}
             [:input {:value (:api-key credential)
                      :class "key-display"
                      :read-only true}]
             [copy-text
              {:text (:api-key credential)}
              [:a {:class "icon-copy"}]]]]
           [:div {:class "api-key-col"} "Permissions: "
            (map-indexed (fn [idx scope]
                           [:span {:key (str "scope-display-" idx)}
                            (str (when (> idx 0)
                                   ", ")
                                 scope)])
                         scopes)]]
          (when @expanded
            [:div {:class "api-key-expand"}
             [:div {:class "api-key-col"}
              [:p {:class "api-key-col-header"}
               "API Key Secret"]
              [:div {:class "action-row"}
               (when @show-secret
                 [:div {:class "action-label-wide"}
                  [:span
                   [:input {:value (:secret-key credential)
                            :class "key-display"
                            :read-only true}]
                   [copy-text
                    {:text (:secret-key credential)}
                    [:a {:class "icon-copy"}]]]])
               [:ul {:class "action-icon-list"}
                [:li
                 [:a {:href "#!",
                      :class "icon-secret"
                      :on-click #(swap! show-secret not)}
                  (str (cond
                         @show-secret "Hide"
                         :else "Show Secret Key"))]]]]]
             [:div {:class "api-key-col"}
              [:p {:class "api-key-col-header"}
               "Permissions"]
              (cond
                @edit
                [:div {:class "action-row"}
                 [:ul {:class "role-select"}
                  (map (fn [scope]
                         [:li {:class "role-checkbox"
                               :key (str "permission-check-"
                                         idx "-" scope)}
                          [:input {:type "checkbox",
                                   :name "scopes",
                                   :value scope
                                   :checked (has-scope scopes scope)
                                   :on-change #(dispatch [:credentials/update-credential
                                                          idx
                                                          (assoc credential :scopes
                                                                 (toggle-scope scope scopes))])}]
                          [:label {:for "scopes"} (str " " scope)]])
                       ["statements/write" "statements/read" "all/read" "all"])]
                 [:ul {:class "action-icon-list"}
                  [:li
                   [:a {:href "#!",
                        :on-click (fn []
                                    (swap! edit not)
                                    (dispatch [:credentials/save-credential credential]))
                        :class "icon-save"} "Save"]]
                  [:li
                   [:a {:href "#!",
                        :on-click (fn []
                                    (swap! edit not)
                                    (dispatch [:credentials/load-credentials]))
                        :class "icon-close"} "Cancel"]]]]
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
                   [:a {:href "#!",
                        :on-click #(dispatch [:credentials/delete-credential credential])
                        :class "icon-delete"} "Delete"]]]])]])]]))))
