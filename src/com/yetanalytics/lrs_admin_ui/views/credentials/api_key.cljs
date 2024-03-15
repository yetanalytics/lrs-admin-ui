(ns com.yetanalytics.lrs-admin-ui.views.credentials.api-key
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame :refer [dispatch subscribe]]
   [com.yetanalytics.lrs-admin-ui.functions.copy   :refer [copy-text]]
   [com.yetanalytics.lrs-admin-ui.functions.scopes :refer [scope-list has-scope? toggle-scope]]
   [com.yetanalytics.lrs-admin-ui.functions        :refer [ps-event]]))

(defn api-key
  [{:keys [idx]}]
  (let [expanded (r/atom false)
        show-secret (r/atom false)
        edit (r/atom false)
        delete-confirm (r/atom false)]
    (fn []
      (let [credential    @(subscribe [:credentials/get-credential idx])
            scopes        (:scopes credential)
            scope-display (map-indexed (fn [idx scope]
                                         [:span {:key (str "scope-display-" idx)}
                                          (str (when (> idx 0)
                                                 ", ")
                                               scope)])
                                       scopes)]
        [:li {:class "mb-2"}
         [:div {:class "accordion-container"}
          [:div {:class "api-key-row"
                 :aria-label (subscribe [:lang/get :notification.key.aria])
                 :on-click #(swap! expanded not)}
           [:div {:class "api-key-col"}
            [:span {:class (str "collapse-sign"
                                (when @expanded " expanded"))}
             [:input {:value (:api-key credential)
                      :class "key-display"
                      :read-only true}]
             [copy-text
              {:text (:api-key credential)
               :on-copy #(dispatch [:notification/notify false 
                                    @(subscribe [:lang/get :notification.credentials.key-copied])])}
              [:a {:class "icon-copy"
                   :on-click #(ps-event %)}]]]]
           [:div {:class "api-key-col"} @(subscribe [:lang/get :credentials.key.permissions]) scope-display]]
          (when @expanded
            [:div {:class "api-key-expand"}
             [:div {:class "api-key-col"}
              [:p {:class "api-key-col-header"}
               @(subscribe [:lang/get :credentials.key.secret])]
              [:div {:class "action-row"}
               (when @show-secret
                 [:div {:class "action-label-wide"}
                  [:span
                   [:input {:value (:secret-key credential)
                            :class "key-display"
                            :read-only true}]
                   [copy-text
                    {:text (:secret-key credential)
                     :on-copy #(dispatch [:notification/notify false 
                                          @(subscribe [:lang/get :notification.credentials.secret-copied])])}
                    [:a {:class "icon-copy pointer"}]]]])
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
                        :class "icon-close"} @(subscribe [:lang/get :credentials.key.permissions.cancel])]]]]
                :else
                [:div {:class "action-row"}
                 [:div {:class "action-label"}
                  scope-display]
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
                      @(subscribe [:lang/get :credentials.key.delete])]])]])]])]]))))
