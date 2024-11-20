(ns com.yetanalytics.lrs-admin-ui.views.form
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.views.form.dropdown
             :refer [make-key-down-fn
                     select-input-top
                     dropdown-items
                     combo-box-dropdown]]
            [goog.string :refer [format]]
            [goog.string.format]))

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
   | `tooltip`      | A keyword or string that corresponds to tooltip info when the user hovers over the info icon.
   | `required`     | A boolean that will show a required indicator if true.
   | `err-sub`      | A subscription vector to buffer errors that may include errors corresponding to this component.
   | `err-match`    | An error location vector for filtering errors from err-sub. It is treated as a prefix and will match any error that begins with it.
   | `removable?`   | When true, a \"(None)\" option will appear as the first item in the dropdown; clicking it results in passing `nil` to `on-change`.
   | `remove-text`  | The dropdown label for `nil` when `removable?` is `true`. Default is \"(None)\"."
  [{:keys [id name on-change on-search value placeholder disabled custom-text?
           options-fn
           #_:clj-kondo/ignore tooltip
           #_:clj-kondo/ignore err-sub
           #_:clj-kondo/ignore err-match
           #_:clj-kondo/ignore required
           removable? remove-text]
    :or {name        (random-uuid)
         id          (random-uuid)
         disabled    false
         value       ""
         placeholder "Please make your selection"
         on-change   identity
         on-search   (constantly nil)
         options-fn  (constantly [])
         removable?  false
         remove-text "(None)"}}
   & #_:clj-kondo/ignore label]
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
      (let [opts-coll      (cond->> (vec (options-fn))
                             removable?
                             (into [{:value nil :label remove-text}]))
            on-key-down-fn (make-key-down-fn
                            {:options         opts-coll
                             :dropdown-focus  dropdown-focus
                             :dropdown-open?  dropdown-open?
                             :value-update-fn value-update-fn
                             :space-select?   false})]
        [:div
         #_[form-label label id tooltip required err-sub err-match]
         [:div {:id          id
                :disabled    disabled
                :tab-index   0
                :class       "form-custom-select-input"
                :on-key-down on-key-down-fn
                :on-blur     on-blur-fn
                :aria-label  (format "Combo Box Input for %s" name)}
          [select-input-top
           {:id             id
            :name           name
            :disabled       disabled
            :options        opts-coll
            :current-value  current-value
            :dropdown-open? dropdown-open?
            :placeholder    placeholder}]
          (when (and (not disabled) @dropdown-open?)
            [combo-box-dropdown
             {:id               id
              :name             name
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
               icon-src    @(subscribe [:resources/icon "add.svg"])
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
