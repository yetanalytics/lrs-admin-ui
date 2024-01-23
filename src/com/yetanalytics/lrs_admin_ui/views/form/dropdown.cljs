(ns com.yetanalytics.lrs-admin-ui.views.form.dropdown
  (:require [com.yetanalytics.lrs-admin-ui.functions :as fns]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-key-down-fn
  "Return an event callback function to use for `:on-key-down`."
  [{:keys [options dropdown-focus dropdown-open? value-update-fn space-select?]}]
  (let [opts-count     (count options)
        opts-dec-count (dec opts-count)
        on-enter       (fn [_]
                         (when (not-empty options)
                           (-> options
                               (get @dropdown-focus)
                               :value
                               value-update-fn)))
        on-up          (fn [e]
                         (when (<= 0 (dec @dropdown-focus))
                           (swap! dropdown-focus dec))
                         (fns/ps-event e))
        on-down        (fn [e]
                         (when (< (inc @dropdown-focus) opts-count)
                           (swap! dropdown-focus inc))
                         (fns/ps-event e))
        on-pg-up       (fn [e]
                         (reset! dropdown-focus 0)
                         (fns/ps-event e))
        on-pg-down     (fn [e]
                         (reset! dropdown-focus opts-dec-count)
                         (fns/ps-event e))]
    (fn [e]
      (case (fns/get-event-key e)
        :space     (if-not @dropdown-open?
                     (do (reset! dropdown-open? true)
                         (fns/ps-event e))
                     (when space-select?
                       (on-enter e)))
        :enter     (on-enter e)
        :escape    (reset! dropdown-open? false)
        :arrowup   (on-up e)
        :arrowdown (on-down e)
        :pageup    (on-pg-up e)
        :pagedown  (on-pg-down e)
        :home      (on-pg-up e)
        :end       (on-pg-down e)
        nil))))

(defn- get-label
  "Takes an options coll and a value and returns the label text"
  [opts val]
  (->> opts
       (filter #(= (:value %) val))
       first
       :label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dropdown Component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dropdown-items
  "The list of items in a dropdown."
  [{:keys [id name options dropdown-focus value-update-fn]}]
  (into [:ul {:id       (str id "-dropdown-items")
              :name     (str name "-dropdown-items")
              :class    "form-select-dropdown-items"}]
        (map-indexed
         (fn [idx {:keys [value label]}]
           [:li {:id            (str id "-dropdown-item-" idx)
                 :class         (if (= @dropdown-focus idx)
                                  "form-select-dropdown-item selected"
                                  "form-select-dropdown-item")
                 :on-mouse-over #(reset! dropdown-focus idx)
                 :on-click      #(value-update-fn value)
                 :aria-label    (str "Select the value " label)}
            [:p label]])
         options)))

(defn- combo-box-search
  "The top combo box search bar."
  [{:keys
    [id name dropdown-value value-update-fn search-update-fn custom-text?]}]
  [:div
   [:div {:class "form-select-dropdown-search-label"}
    (if custom-text? [:p "Search or Add:"] [:p "Search:"])]
   [:div {:class "form-select-dropdown-search-box"}
    [:input {:on-change (fn [x]
                          (reset! dropdown-value (fns/ps-event-val x))
                          (search-update-fn @dropdown-value))
             :type      "text"
             :value     @dropdown-value
             :id        (str id "-dropdown-search")
             :name      (str name "-dropdown-search")
             :class     (if custom-text?
                          "form-text-input-with-side-button"
                          "form-text-input")}]
    (when custom-text?
      [:span {:class       "side-button"
              :on-click    (fn [_]
                             (value-update-fn @dropdown-value))
              :on-key-down (fn [e]
                             (when (= :enter (fns/get-event-key e))
                               (value-update-fn @dropdown-value)
                               (fns/ps-event e)))
              :tab-index   0
              :aria-label  "Select the text in the search bar."}
       [:img {:src "images/icons/icon-add.svg"}]
       "Add"])]])

(defn combo-box-dropdown
  "A dropdown specific for combo boxes, including the search bar."
  [{:keys [id name] :as opts}]
  [:div {:id    (str id "-dropdown")
         :name  (str name "-dropdown")
         :class "form-select-dropdown"}
   [combo-box-search opts]
   [dropdown-items opts]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Top Component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-input-top
  "The top pane of a (singleton) select input (including regular combo boxes)."
  [{:keys [id name disabled placeholder options current-value dropdown-open?]}]
  [:div {:id       (str id "-select-input")
         :name     (str name "-select-input")
         :class    (cond
                     disabled        "form-select-top disabled"
                     @dropdown-open? "form-select-top opened"
                     :else           "form-select-top")
         :on-click #(when-not disabled (swap! dropdown-open? not))}
   [:span {:class "form-select-top-left"}
    [:p (if-some [value @current-value]
            ;; TODO: Better solution to value-label discrepancy
          (or (get-label options value) value)
          placeholder)]]
   [:span {:class "form-select-top-right"}
    [:img {:src (if @dropdown-open?
                  "images/icons/icon-expand-less.svg"
                  "images/icons/icon-expand-more.svg")}]]])

(defn multi-select-input-top
  "The top pane of a multiple-selection combo box."
  [{:keys [id name disabled options current-value dropdown-open?
           value-update-fn placeholder]}]
  [:div {:id    (str id "-multi-select-input")
         :name  (str name "-multi-select-input")
         :class (if @dropdown-open?
                  "form-multi-select-top opened"
                  "form-multi-select-top")
         ; We need two ps-event calls to stop propagation of onClick
         ;; event from the div to the delete button
         :on-click (fn [e]
                     (when-not disabled
                       (swap! dropdown-open? not)
                       (fns/ps-event e)))}
   [:div {:class "form-multi-select-top-left"}
    (if-some [values (not-empty @current-value)]
      (reduce
       (fn [acc val]
         (conj acc
               [:span {:class "form-multi-select-array-item"}
                ;; TODO: Better solution to value-label discrepancy
                [:p (or (get-label options val) val)
                 [:img {:src "images/icons/icon-close-black.svg"
                        :on-click (fn [e]
                                    (fns/ps-event e)
                                    (value-update-fn val))}]]]))
       [:span]
       values)
      [:p placeholder])]
   [:div {:class "form-multi-select-top-right"}
    [:img {:src (if @dropdown-open?
                  "images/icons/icon-expand-less.svg"
                  "images/icons/icon-expand-more.svg")}]]])
