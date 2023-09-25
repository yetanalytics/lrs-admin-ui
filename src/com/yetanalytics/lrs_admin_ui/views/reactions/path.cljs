(ns com.yetanalytics.lrs-admin-ui.views.reactions.path
  (:require [reagent.core :as r]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-admin-ui.views.form :as form]
            [goog.string :refer [format]]
            [goog.string.format]))

(defn- path-input-segment
  [path-until
   seg-val]
  (let [id (str (random-uuid))]
    [form/combo-box-input
     {:id id
      :name (format "combo-%s" id)
      :on-change println
      :on-search println
      :value seg-val
      ;; :placehoder "select some sheeeeeit"
      :disabled false
      :custom-text? true
      :options-fn
      (fn []
        (let [{:keys [next-keys]} (rfns/analyze-path
                                   rfns/pathmap-statement
                                   path-until)]
          (if (= ['idx] next-keys)
            ;; index expected
            (for [idx (range 10)]
              {:label (str idx) :value idx})
            (for [k next-keys]
              {:label k :value k}))))
      ;; :tooltip "I'M A TOOLTIP OVA HEA" ;; NOT YET IMPLEMENTED, MIGHT NEVER BE
      ;; :required true
      :removable? false}]))

(defn path-input
  []
  (let [state (r/atom {:add? false})
        add? (r/cursor state [:add?])]
    (fn [path]
      (let [{:keys [next-keys
                    leaf-type]} (rfns/analyze-path
                                 rfns/pathmap-statement
                                 path)]
        (-> [:div.path-input]
            (into
             (map
              (fn [idx]
                (let [[path-until [seg-val]] (split-at idx path)]
                  [path-input-segment
                   path-until
                   (or seg-val "")]))
              (range (count path))))
            (conj
             (if (nil? leaf-type)
               (if @add?
                 [:<>
                  [path-input-segment
                   path
                   ""]
                  [:a {:href "#"
                       :on-click (fn [e]
                                   (fns/ps-event e)
                                   (swap! state assoc :add? false))}
                   [:img {:src "/images/icons/icon-close-blue.svg"}]]]
                 [:a {:href "#"
                      :on-click (fn [e]
                                  (fns/ps-event e)
                                  (swap! state assoc :add? true))}
                  [:img {:src "/images/icons/icon-add.svg"}]])
               [:div.path-leaf-type (format "(%s)" (str leaf-type))])))))))

(defn formtest
  []
  [:div
   [:h5 (str ["actor" "mbox"])]
   [:p "completed path with type"]
   [path-input ["actor" "mbox"]]

   [:h5 (str ["actor" "account"])]
   [:p "incomplete path"]
   [path-input ["actor" "account"]]

   [:h5 (str ["actor" "member"])]
   [:p "incomplete array path"]
   [path-input ["actor" "member"]]]

  #_[form/combo-box-input
   {:id "foo"
    :name "combo"
    :on-change println
    :on-search println
    :value ""
    :placehoder "select some sheeeeeit"
    :disabled false
    :custom-text? true
    :options-fn #(vector {:label "Foo" :value "foo"}
                         {:label "Bar" :value "bar"}
                         {:label "Baz" :value "baz"})
    :tooltip "I'M A TOOLTIP OVA HEA" ;; NOT YET IMPLEMENTED, MIGHT NEVER BE
    :required true
    :removable? false}])
