(ns com.yetanalytics.lrs-admin-ui.views.form
  (:require [reagent.core :as r]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.views.form.dropdown
             :refer [make-key-down-fn
                     select-input-top
                     dropdown-items
                     combo-box-dropdown]]))

;; Combo Box Input ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn combo-box-input
  "A combo box for selecting a single item.

   | Key | Description
   | --- | ---
   | `id`           | The ID of the combo box in the DOM.
   | `name`         | The name of the combo box.
   | `on-change`    | A callback function that is called when the user makes a selection (e.g. by clicking on an item or on \"Add\" for custom item).
   | `on-search`    | A callback function that is called whenever an update is made in the search bar.
   | `value`        | The initial value. Default is the empty string.
   | `placeholder`  | The placeholder text for when an item has not yet been selected.
   | `disabled`     | Is the combo box disabled? If so, the dropdown can't be opened.
   | `custom-text?` | Is the user allowed to input custom text, and not just the select options?
   | `options-fn`   | A thunk that returns the options list.
   | `required`     | A boolean that will show a required indicator if true."
  [{:keys [id on-change on-search value placeholder disabled custom-text?
           options-fn]
    :or {id          (random-uuid)
         disabled    false
         value       ""
         placeholder "Please make your selection"
         on-change   identity
         on-search   (constantly nil)
         options-fn  (constantly [])}}]
  (let [combo-box-ratom (r/atom {:current-value value
                                 :dropdown {:open? false
                                            :focus 0
                                            :value nil}})
        current-value   (r/cursor combo-box-ratom [:current-value])
        dropdown-open?  (r/cursor combo-box-ratom [:dropdown :open?])
        dropdown-focus  (r/cursor combo-box-ratom [:dropdown :focus])
        dropdown-value  (r/cursor combo-box-ratom [:dropdown :value])
        value-update-fn (fn [value]
                          (reset! current-value value)
                          (reset! dropdown-open? false)
                          (on-change @current-value))
        on-blur-fn      (fn [e]
                          (when-not (fns/child-event? e)
                            (reset! dropdown-open? false)))]
    (fn []
      (let [opts-coll      (vec (options-fn))
            on-key-down-fn (make-key-down-fn
                            {:options         opts-coll
                             :dropdown-focus  dropdown-focus
                             :dropdown-open?  dropdown-open?
                             :value-update-fn value-update-fn
                             :space-select?   false})]
        [:div
         [:div {:id          id
                :disabled    disabled
                :tab-index   0
                :class       "form-custom-select-input"
                :on-key-down on-key-down-fn
                :on-blur     on-blur-fn
                :aria-label  "Combo Box Input"}
          [select-input-top
           {:id             id
            :disabled       disabled
            :options        opts-coll
            :current-value  current-value
            :dropdown-open? dropdown-open?
            :placeholder    placeholder}]
          (when (and (not disabled) @dropdown-open?)
            [combo-box-dropdown
             {:id               id
              :dropdown-focus   dropdown-focus
              :dropdown-value   dropdown-value
              :value-update-fn  value-update-fn
              :search-update-fn on-search
              :options          opts-coll
              :custom-text?     custom-text?}])]]))))

;; Action Dropdown ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn action-dropdown
  "Icon that, when clicked, presents a dropdown of clickable actions."
  [_]
  (let [state (r/atom {:dropdown-open? false
                       :dropdown-focus nil})
        dropdown-open? (r/cursor state [:dropdown-open?])
        dropdown-focus (r/cursor state [:dropdown-focus])
        id (str (random-uuid))]
    (fn [{:keys [options
                 select-fn
                 label
                 label-left?
                 class
                 icon-src]
          :or {options     []
               select-fn   (fn [v] (println 'select v))
               label-left? false
               icon-src    "images/icons/add.svg"
               class       ""}}]
      [:div.action-dropdown
       {:on-blur (fn [_]
                   ;; FIXME: Horrible hack, can't figure out how to stop the clobbering here
                   (js/setTimeout #(swap! state assoc :dropdown-open? false) 200))}
       [:div.action-dropdown-icon
        [:a {:href "#"
             :on-click (fn [e]
                         (fns/ps-event e)
                         (swap! state assoc :dropdown-open? true))}
         (when (and label label-left?)
           [:span.action-dropdown-label (str label " ")])
         [:img {:src icon-src}]
         (when (and label (not label-left?))
           [:span.action-dropdown-label (str " " label)])]]
       [:div.action-dropdown-list
        {:class (str class (if @dropdown-open? " dropdown-open" ""))}
        [dropdown-items
         {:id id
          :name id
          :dropdown-focus dropdown-focus
          :value-update-fn (fn [v]
                             (select-fn v))
          :options options}]]])))
