(ns com.yetanalytics.lrs-admin-ui.views.credentials.api-key
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame :refer [dispatch subscribe]]
   [com.yetanalytics.lrs-admin-ui.functions.copy   :refer [copy-text]]
   [com.yetanalytics.lrs-admin-ui.functions.scopes :refer [scope-list has-scope? toggle-scope]]
   [com.yetanalytics.lrs-admin-ui.functions        :refer [ps-event]]))

(defn- scope-list-text
  "Convert scopes into a comma-separated list, e.g. \"all, all-read\"."
  [scopes]
  (map-indexed (fn [idx scope]
                 [:span {:key (str "scope-display-" scope)}
                  (str (when (> idx 0)
                         ", ")
                       scope)])
               scopes))

(defn- api-key-row
  [{:keys [api-key label scopes] :as _credential} expanded]
  [:div {:class      "api-key-row"
         :aria-label @(subscribe [:lang/get :notification.key.aria])
         :on-click   #(swap! expanded not)}
   ;; API Key View
   [:div {:class "api-key-col"}
    [:span {:class (str "collapse-sign"
                        (when @expanded " expanded"))}
     [:input {:value     api-key
              :class     "key-display"
              :read-only true}]
     [copy-text
      {:text    api-key
       :on-copy #(dispatch [:notification/notify false
                            @(subscribe [:lang/get :notification.credentials.key-copied])])}
      [:a {:class "icon-copy"
           :on-click #(ps-event %)}]]]]
   ;; Label
   [:div {:class "api-key-col"}
    "Label: " [:span (or label "(None)")]]
   ;; Permissions
   [:div {:class "api-key-col"}
    @(subscribe [:lang/get :credentials.key.permissions])
    (scope-list-text scopes)]])

(defn- api-key-expand-edit
  [idx {:keys [scopes] :as credential} edit]
  [:div {:class "action-row"}
   [:ul {:class "role-select"}
    (map (fn [scope]
           [:li {:class "role-checkbox"
                 :key (str "permission-check-"
                           idx "-" scope)}
            [:input {:type "checkbox",
                     :name "scopes",
                     :value scope
                     :checked (has-scope? scopes scope)
                     :on-change #(dispatch [:credentials/update-credential
                                            idx
                                            (assoc credential :scopes
                                                   (toggle-scope scopes scope))])}]
            [:label {:for "scopes"} (str " " scope)]])
         scope-list)]
   [:ul {:class "action-icon-list"}
    [:li
     [:a {:href "#!",
          :on-click (fn []
                      (swap! edit not)
                      (dispatch [:credentials/save-credential credential]))
          :class "icon-save"} @(subscribe [:lang/get :credentials.key.permissions.save])]]
    [:li
     [:a {:href "#!",
          :on-click (fn []
                      (swap! edit not)
                      (dispatch [:credentials/load-credentials]))
          :class "icon-close"} @(subscribe [:lang/get :credentials.key.permissions.cancel])]]]])

(defn- api-key-expand-view
  [{:keys [scopes] :as credential} edit]
  (let [delete-confirm (r/atom false)]
    (fn [_credential _edit]
      [:div {:class "action-row"}
       [:div {:class "action-label"}
        (scope-list-text scopes)]
       [:ul {:class "action-icon-list"}
        [:li
         [:a {:href "#!",
              :on-click #(swap! edit not)
              :class "icon-edit"} @(subscribe [:lang/get :credentials.key.edit])]]
        (if @delete-confirm
          [:li
           [:span @(subscribe [:lang/get :credentials.key.delete.confirm])]
           [:a {:href "#!",
                :on-click #(do (dispatch [:credentials/delete-credential credential])
                               (swap! delete-confirm not))
                :class "confirm-delete"}
            "Yes"]
           [:a {:href "#!"
                :on-click #(swap! delete-confirm not)
                :class "confirm-delete"}
            "No"]]
          [:li
           [:a {:href "#!"
                :on-click #(swap! delete-confirm not)
                :class "icon-delete"}
            @(subscribe [:lang/get :credentials.key.delete])]])]])))

(defn- api-key-expand-secret-key
  [{:keys [secret-key] :as _credential}]
  [:div {:class "action-label-wide"}
   [:span
    [:input {:value secret-key
             :class "key-display"
             :read-only true}]
    [copy-text
     {:text    secret-key
      :on-copy #(dispatch [:notification/notify false
                           @(subscribe [:lang/get :notification.credentials.secret-copied])])}
     [:a {:class "icon-copy pointer"}]]]])

(defn- api-key-expand
  [idx credential]
  (let [edit        (r/atom false)
        show-secret (r/atom false)]
    (fn [_idx _credential _delete-confirm]
      [:div {:class "api-key-expand"}
       [:div {:class "api-key-col"}
        [:p {:class "api-key-col-header"}
         @(subscribe [:lang/get :credentials.key.secret])]
        [:div {:class "action-row"}
         (when @show-secret
           [api-key-expand-secret-key credential])
         [:ul {:class "action-icon-list"}
          [:li
           [:a {:href "#!",
                :class "icon-secret"
                :on-click #(swap! show-secret not)}
            (str (cond
                   @show-secret @(subscribe [:lang/get :credentials.key.hide])
                   :else @(subscribe [:lang/get :credentials.key.show])))]]]]]
       [:div {:class "api-key-col"}
        [:p {:class "api-key-col-header"}
         @(subscribe [:lang/get :credentials.key.permissions])]
        (if @edit
          [api-key-expand-edit idx credential edit]
          [api-key-expand-view credential edit])]])))

(defn api-key
  [{:keys [idx]}]
  (let [expanded (r/atom false)]
    (fn []
      (let [credential @(subscribe [:credentials/get-credential idx])]
        [:li {:class "mb-2"}
         [:div {:class "accordion-container"}
          [api-key-row credential expanded]
          (when @expanded
            [api-key-expand idx credential])]]))))
