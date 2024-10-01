(ns com.yetanalytics.lrs-admin-ui.views.form.dropdown
  (:require [reagent.core :as r]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-key-down-fn*
  [{:keys [dropdown-open?]}]
  (fn [e]
    (case (fns/get-event-key e)
      :space (when-not @dropdown-open?
               (reset! dropdown-open? true)
               (fns/ps-event e)))))

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

(defn get-label
  "Takes an options coll and a value and returns the label text"
  [opts val]
  (->> opts
       (filter #(= (:value %) val))
       first
       :label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dropdown Component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- add-button
  [{:keys [dropdown-value value-update-fn]}]
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
   "Add"])

(defn dropdown-items
  "The list of items in a dropdown."
  [{:keys [id options dropdown-focus value-update-fn]}]
  (into [:ul {:id       (str id "-dropdown-items")
              :name     (str id "-dropdown-items")
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
    [id dropdown-value value-update-fn on-search]}]
  (let [value-ref (r/atom @dropdown-value)]
    (fn [_]
      [:div
       [:div {:class "form-select-dropdown-search-label"}
        [:p "Search or Add:"]]
       [:div {:class "form-select-dropdown-search-box"}
        [:input {:on-change (fn [x]
                              (let [v (fns/ps-event-val x)]
                                (reset! value-ref v)
                                (on-search v)))
                 :type      "text"
                 :value     @value-ref
                 :id        (str id "-dropdown-search")
                 :name      (str id "-dropdown-search")
                 :class     "form-text-input-with-side-button"}]
        [add-button {:dropdown-value  value-ref
                     :value-update-fn value-update-fn}]]])))

(defn combo-box-dropdown
  "A dropdown specific for combo boxes, including the search bar."
  [{:keys [id name] :as opts}]
  [:div {:id    (str id "-dropdown")
         :name  (str name "-dropdown")
         :class "form-select-dropdown"}
   [combo-box-search opts]
   [dropdown-items opts]])

(defn combo-box-numeric
  [{:keys [min dropdown-value value-update-fn]}]
  (let [value-ref (r/atom @dropdown-value)]
    (fn [_]
      [:div {:class "form-select-dropdown"}
       [:div {:class "form-select-dropdown-search-box"}
        [:input {:on-change (fn [x]
                              (reset! value-ref (fns/ps-event-val x)))
                 :type      "number"
                 :min       min
                 :value     @value-ref
                 :class     "form-text-input-with-side-button"}]
        [add-button {:dropdown-value  value-ref
                     :value-update-fn value-update-fn}]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Top Component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-input-top
  "The top pane of a (singleton) select input (including regular combo boxes)."
  [{:keys [id disabled label placeholder dropdown-open?]}]
  [:div {:id       (str id "-select-input")
         :name     (str id "-select-input")
         :class    (cond
                     disabled        "form-select-top disabled"
                     @dropdown-open? "form-select-top opened"
                     :else           "form-select-top")
         :on-click #(when-not disabled (swap! dropdown-open? not))}
   [:span {:class "form-select-top-left"}
    [:p (or label placeholder)]]
   [:span {:class "form-select-top-right"}
    [:img {:src (if @dropdown-open?
                  "images/icons/icon-expand-less.svg"
                  "images/icons/icon-expand-more.svg")}]]])

(defn action-select-top
  "The top pane of a select input with a custom dropdown icon."
  [{:keys [label label-left? icon-src dropdown-open?]}]
  [:div.action-dropdown-icon
   [:a {:href "#"
        :on-click (fn [e]
                    (fns/ps-event e)
                    (reset! dropdown-open? true))}
    (when (and label label-left?)
      [:span.action-dropdown-label (str label " ")])
    [:img {:src icon-src}]
    (when (and label (not label-left?))
      [:span.action-dropdown-label (str " " label)])]])
