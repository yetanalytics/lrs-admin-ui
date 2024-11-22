(ns com.yetanalytics.lrs-admin-ui.views.form
  (:require [reagent.core :as r]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.views.form.dropdown
             :as drop-form
             :refer [make-key-down-fn*
                     make-key-down-fn
                     select-input-top
                     items-dropdown
                     combo-box-dropdown
                     numeric-dropdown
                     action-select-top]]))

;; Combo Box Input ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn combo-box-input
  "A combo box for selecting a single item.

   | Key | Description
   | --- | ---
   | `id`           | The ID of the combo box in the DOM.
   | `on-change`    | A callback function that is called when the user makes a selection (e.g. by clicking on an item or on \"Add\" for custom item).
   | `on-filter`    | A callback that is called whenever the search box is updated, that filters out the options list.
   | `value`        | The initial value. Default is the empty string.
   | `placeholder`  | The placeholder text for when an item has not yet been selected.
   | `disabled`     | Is the combo box disabled? If so, the dropdown can't be opened.
   | `options`      | The options list of `{:label ... :value ...}` maps."
  [{:keys [id on-change on-filter value placeholder disabled options]
    :or {id          (random-uuid)
         disabled    false
         value       ""
         placeholder "Please make your selection"
         on-change   identity
         on-filter   (fn [opts _] opts)
         options     []}}]
  (let [options-ref     (r/atom options)
        combo-box-ratom (r/atom {:current-value value
                                 :dropdown {:open? false
                                            :focus 0
                                            :value nil}})
        current-value   (r/cursor combo-box-ratom [:current-value])
        dropdown-open?  (r/cursor combo-box-ratom [:dropdown :open?])
        dropdown-focus  (r/cursor combo-box-ratom [:dropdown :focus])
        dropdown-value  (r/cursor combo-box-ratom [:dropdown :value])
        on-enter        (fn [value]
                          (reset! current-value value)
                          (reset! dropdown-open? false)
                          (on-change @current-value))
        on-blur         (fn [e]
                          (when-not (fns/child-event? e)
                            (reset! dropdown-open? false)))]
    (fn [_opts]
      (let [options*       @options-ref
            curr-label     (or (drop-form/get-label options* @current-value)
                               (str @current-value))
            on-key-down    (make-key-down-fn
                            {:options        options*
                             :dropdown-focus dropdown-focus
                             :dropdown-open? dropdown-open?
                             :on-enter       on-enter
                             :space-select?  false})]
        [:div {:id          id
               :disabled    disabled
               :tab-index   0
               :class       "form-custom-select-input"
               :on-key-down on-key-down
               :on-blur     on-blur
               :aria-label  "Combo Box Input"}
         [select-input-top
          {:id             id
           :disabled       disabled
           :dropdown-open? dropdown-open?
           :label          curr-label
           :placeholder    placeholder}]
         (when (and (not disabled) @dropdown-open?)
           [combo-box-dropdown
            {:id             id
             :dropdown-focus dropdown-focus
             :dropdown-value dropdown-value
             :on-enter       on-enter
             :on-search      (fn [search-str]
                               (reset! options-ref
                                       (on-filter options search-str)))
             :options        options*}])]))))

;; Numeric Input ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn numeric-input
  "A dropdown that displays a numeric input to add.

   | Key | Description
   | --- | ---
   | `id`           | The ID of the dropdown in the DOM.
   | `min`          | The minimum numeric value the value can be (as a string).
   | `on-change`    | A callback function that is called when the user makes a selection (e.g. by clicking on an item or on \"Add\" for custom item).
   | `value`        | The initial value. Default is the empty string.
   | `placeholder`  | The placeholder text for when an item has not yet been selected.
   | `disabled`     | Is the combo box disabled? If so, the dropdown can't be opened."
  [{:keys [id min on-change value placeholder disabled]
    :or {id          (random-uuid)
         disabled    false
         value       ""
         placeholder "Please make your selection"
         on-change   identity}}]
  (let [combo-box-ratom (r/atom {:current-value value
                                 :dropdown {:open? false
                                            :focus 0
                                            :value nil}})
        current-value   (r/cursor combo-box-ratom [:current-value])
        dropdown-open?  (r/cursor combo-box-ratom [:dropdown :open?])
        on-enter        (fn [value]
                          (reset! current-value value)
                          (reset! dropdown-open? false)
                          (on-change @current-value))
        on-blur-fn      (fn [e]
                          (when-not (fns/child-event? e)
                            (reset! dropdown-open? false)))]
    (fn [_opts]
      (let [curr-label     (str @current-value)
            on-key-down-fn (make-key-down-fn*
                            {:dropdown-open?  dropdown-open?})]
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
           :dropdown-open? dropdown-open?
           :label          curr-label
           :placeholder    placeholder}]
         (when (and (not disabled) @dropdown-open?)
           [numeric-dropdown
            {:id             id
             :min            min
             :dropdown-value current-value
             :on-enter       on-enter}])]))))

;; Action Dropdown ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn action-dropdown
  "Icon that, when clicked, presents a dropdown of clickable actions.
   
    | Key | Description
   | --- | ---
   | `select-fn`   | Callback that is triggered when an item is selected from the dropdown.
   | `label`       | Dropdown label
   | `label-left?` | Whether the label should be placed to the left of the icon.
   | `options`     | The ID of the combo box in the DOM.
   | `icon-name`   | The file name of the icon.
   | `class`       | Custom CSS class name."
  [{:keys [select-fn
           label
           label-left?
           options
           icon-name
           class]
    :or {options     []
         select-fn   (fn [v] (println 'select v))
         label-left? false
         icon-name   "add.svg"
         class       ""}}]
  (let [state (r/atom {:dropdown-open? false
                       :dropdown-focus nil})
        dropdown-open? (r/cursor state [:dropdown-open?])
        dropdown-focus (r/cursor state [:dropdown-focus])
        id (str (random-uuid))]
    (fn [_opts]
      [:div.action-dropdown
       {:on-blur (fn [_]
                   ;; FIXME: Horrible hack, can't figure out how to stop the clobbering here
                   (js/setTimeout #(swap! state assoc :dropdown-open? false) 200))}
       [action-select-top
        {:label          label
         :label-left?    label-left?
         :icon-name      icon-name
         :dropdown-open? dropdown-open?}]
       [:div.action-dropdown-list
        {:class (str class (if @dropdown-open? " dropdown-open" ""))}
        [items-dropdown
         {:id             id
          :name           id
          :dropdown-focus dropdown-focus
          :on-enter       select-fn
          :options        options}]]])))
