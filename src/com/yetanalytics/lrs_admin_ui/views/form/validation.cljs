(ns com.yetanalytics.lrs-admin-ui.views.form.validation
  (:require
   [reagent.core :as r]
   [goog.string]))

(defn- unescape
  "Alias for `goog.string/unescapeEntities`, where `s` is the string to
   unescape, e.g. an `&`-prefixed string representing a special char."
  [s]
  (goog.string/unescapeEntities s))

(defn- type->div-class
  [validation-type]
  (case validation-type
    :valid "validation-display"
    :warning "validation-display warning"
    :error "validation-display invalid"
    :invalid "validation-display invalid"
    :loading "validation-display loading"))

(defn- type->icon-path
  [validation-type]
  (case validation-type
    :valid "/images/icons/valid.svg"
    :warning "/images/icons/warning.svg"
    :error "/images/icons/invalid.svg"
    :invalid "/images/icons/invalid.svg"
    :loading "/images/icons/loading.svg"))

(defn- type->item-icon-path
  [validation-type]
  (case validation-type
    :warning "/images/icons/warning-item.svg"
    :error "/images/icons/error-item.svg"
    :invalid "/images/icons/error-item.svg"))

(defn validation-static-display
  [validation-type message]
  [:div {:class (type->div-class validation-type)}
   [:span {:class "validation-status"}
    [:img {:src (type->icon-path validation-type)}]
    message]])

(defn validation-display
  [validation-type message status item-display]
  (let [open (r/cursor status [:open])]
    [:<>
     [:div {:class (type->div-class validation-type)}
      [:span {:class "validation-status pointer"
              :on-click #(swap! open not)}
       [:img {:src (type->icon-path validation-type)}]
       (str message " " (if @open (unescape "&#9650;") (unescape "&#9660;")))]]
     (when @open
       item-display)]))

(defn validation-item-display
  [icon-path expand-msg status item-msg item-details items]
  (let [expand-id (r/cursor status [:expand-id])]
    [:div {:class "validation-error-wrapper"}
     [:div {:class "validation-errors"
            :on-mouse-leave #(reset! status
                                     {:open      false
                                      :expand-id nil})}
      [:p [:i expand-msg]]
      [:ul
       (let [;; this declaration seems unnecessary but without it the
             ;; list won't get redrawn with the details immediately!
             expand-id @expand-id]
         (map-indexed
          (fn [idx item]
            ^{:key (str "validation-item-" idx)}
            [:li {:class    "pointer"
                  :on-click #(swap! status assoc :expand-id idx)}
             [:p [:img {:src (type->item-icon-path icon-path)}]
              (item-msg item)]
             (when (= expand-id idx)
               [item-details item])])
          items))]]]))
