(ns com.yetanalytics.lrs-admin-ui.views.reactions.path
  (:require [reagent.core :as r]
            [clojure.string :as cstr]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-reactions.path :as rpath]
            [com.yetanalytics.lrs-admin-ui.views.form :as form]
            [goog.string :refer [format]]
            [goog.string.format]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]))

(defn- path-input-segment
  [seg-val]
  [:div.path-input-segment
   (if (number? seg-val)
     (format "[%s]" seg-val)
     (str seg-val))])

(defn- parse-selection [v]
  (let [parsed-int (js/parseInt v)]
    (if (js/isNaN parsed-int)
      (if (nil? v)
        ""
        v)
      parsed-int)))

(defn- next-key-options [next-key-opts search-str]
  (->> next-key-opts
       (filter (fn [{:keys [value]}] (cstr/starts-with? value search-str)))
       rfns/order-select-entries))

(defn- path-input-segment-edit
  [_ _ _]
  (let [search (r/atom "")]
    (fn [path-until
         seg-val
         change-fn]
      (let [id (str (random-uuid))
            {:keys [next-keys]} (rpath/analyze-path
                                 path-until)]
        [:div.path-input-segment-edit
         [:div.segment-combo
          (if (= '[idx] next-keys)
            ;; When we know it is an index, use a numeric input
            [form/combo-box-numeric-input
             {:id          id
              :min         "0"
              :on-change   (fn [v]
                             (change-fn (parse-selection v)))
              :value       seg-val
              :placeholder "(select)"
              :disabled    false}]
            [form/combo-box-input
             {:id           id
              :name         (format "combo-%s" id)
              :on-change    (fn [v]
                              (change-fn (parse-selection v)))
              :on-search    (fn [v] (reset! search v))
              :options-fn   (fn []
                              (next-key-options next-keys @search))
              :on-filter    next-key-options
              :options      (mapv (fn [k] {:label k :value k}) next-keys)
              :value        seg-val
              :placeholder  "(select)"
              :disabled     false
              :custom-text? true
              ;; :tooltip "I'M A TOOLTIP OVA HEA" ;; NOT YET IMPLEMENTED, MIGHT NEVER BE
              }])]]))))

(defn path-input
  [path
   & {:keys [add-fn
             del-fn
             change-fn
             remove-fn
             validate?]
      :or {add-fn (fn [_] (println 'add))
           del-fn (fn [_] (println 'del))
           change-fn (fn [_] (println 'change))
           validate? true}}]
  (let [{:keys [complete? valid?]} (rpath/analyze-path path)]
    (-> [:div.path-input
         {:class (when (and validate?
                            (or (not valid?) (not complete?)))
                   "invalid")}
         [:div.path-input-root
          "$"]]
        ;; Intermediate path
        (into
         (for [seg (butlast path)]
           [path-input-segment
            seg]))

        (cond->
            ;; Last segment is editable
            (some? (last path))
            (conj
             [path-input-segment-edit
              (butlast path)
              (last path)
              change-fn])
          ;; Offer to delete the last segment if one exists
          (not-empty path)
          (conj
           [:div.path-input-action
            [:a {:href "#"
                 :on-click (fn [e]
                             (fns/ps-event e)
                             (del-fn))}
             [:img {:src "images/icons/minus.svg"}]]])
          ;; Offer another segment if path is valid & not complete
          (and valid?
               (not complete?))
          (conj
           [:div.path-input-action
            [:a {:href "#"
                 :on-click (fn [e]
                             (fns/ps-event e)
                             (add-fn))}
             [:img {:src "images/icons/add.svg"}]]])
          ;; If remove function is provided, add icon for that
          remove-fn
          (conj
           [:div.path-input-action
            [:a {:href "#"
                 :on-click (fn [e]
                             (fns/ps-event e)
                             (remove-fn))}
             [:img {:src "images/icons/icon-delete-brand.svg"}]]])
          )
        ;; Indicate expected type?
        )))

(comment
  ;; Testing helpers
  (defn- add-segment [path]
    (let [{:keys [next-keys]} (rpath/analyze-path
                               path)]
      (conj path
            (if (= '[idx] next-keys)
              0
              (or (first next-keys)
                  "")))))

  (defn- del-segment [path]
    (vec (butlast path)))

  (defn- change-segment [path new-val]
    (assoc path (dec (count path)) new-val))

  (defn formtest
    []
    (let [path (r/atom [])]
      (fn []
        [:div
         [:h5 (str @path)]
         [path-input
          @path
          :add-fn (fn [] (swap! path add-segment))
          :del-fn (fn [] (swap! path del-segment))
          :change-fn (fn [new-val] (swap! path change-segment new-val))]
         ]))


    )
  )
