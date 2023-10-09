(ns com.yetanalytics.lrs-admin-ui.views.reactions.path
  (:require [reagent.core :as r]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-admin-ui.views.form :as form]
            [goog.string :refer [format]]
            [goog.string.format]))

(defn- path-input-segment
  [seg-val]
  [:div.path-input-segment
   (str seg-val)])

(defn- path-input-segment-edit
  [_ _ _]
  (let [search (r/atom "")]
    (fn [path-until
         seg-val
         change-fn]
      (let [id (str (random-uuid))
            {:keys [next-keys]} (rfns/analyze-path
                                 path-until)]
        [:div.path-input-segment-edit
         [form/combo-box-input
          {:id id
           :name (format "combo-%s" id)
           :on-change (fn [v]
                        ;; If it can be an int, pass it as such
                        (let [parsed-int (js/parseInt v)]
                          (if (js/isNaN parsed-int)
                            (change-fn v)
                            (change-fn parsed-int))))
           :on-search (fn [v] (reset! search v))
           :value seg-val
           :placeholder "(select)"
           :disabled false
           :custom-text? true
           :options-fn
           (fn []
             (if (= ['idx] next-keys)
               ;; index expected
               (for [idx (range 10)]
                 {:label (str idx) :value idx})
               (for [k next-keys
                     :when (.startsWith k @search)]
                 {:label k :value k})))
           ;; :tooltip "I'M A TOOLTIP OVA HEA" ;; NOT YET IMPLEMENTED, MIGHT NEVER BE
           ;; :required true
           :removable? false}]]))))

(defn path-input
  [path
   add-fn
   del-fn
   change-fn]
  (let [{:keys [next-keys
                leaf-type
                valid?]} (rfns/analyze-path
                          path)]
    (-> [:div.path-input
         {:class (when-not valid?
                   "invalid")}]
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
             [:img {:src "/images/icons/icon-close-blue.svg"}]]])
          ;; Offer another segment if path is valid & not complete
          (and valid?
               (contains? #{nil 'json} leaf-type))
          (conj
           [:div.path-input-action
            [:a {:href "#"
                 :on-click (fn [e]
                             (fns/ps-event e)
                             (add-fn))}
             [:img {:src "/images/icons/icon-add.svg"}]]]))
        ;; Indicate expected type?
        )))

;; Testing helpers
(defn- add-segment [path]
  (let [{:keys [next-keys]} (rfns/analyze-path
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
        (fn [] (swap! path add-segment))
        (fn [] (swap! path del-segment))
        (fn [new-val] (swap! path change-segment new-val))]]))


  )
