(ns com.yetanalytics.lrs-admin-ui.functions.reaction
  (:require [clojure.set  :refer [rename-keys]]
            [clojure.walk :as w]
            [goog.string  :refer [format]]
            [goog.string.format]
            [xapi-schema.core :as xs]
            [xapi-schema.spec :as xss]))

(defn path->string
  "Given a vector of keys and/or indices, return a JSONPath string suitable for
  SQL JSON access."
  ([path]
   (path->string path "$"))
  ([[seg & rpath] s]
   (if seg
     (recur rpath
            (cond
              (string? seg)
              ;; Unlike on the backend, these don't need to be valid to parse
              (format "%s.%s" s seg)

              (nat-int? seg)
              (format "%s[%d]" s seg)

              :else
              (throw (ex-info "Invalid path segement"
                              {:type    ::invalid-path-segment
                               :segment seg}))))
     s)))

(defn val-type
  "Get a value type as a string"
  [val]
  (cond
    (string? val)
    "string"
    (number? val)
    "number"
    (boolean? val)
    "boolean"
    (nil? val)
    "null"))

(defn validate-template-xapi
  "Take raw JSON str of an xAPI Statement and issue a vec of error maps for 
   any schema violations. Returns `nil` if `raw-json` is valid."
  [raw-json & {:keys [xapi-version]
               :or   {xapi-version "1.0.3"}}]
  (try
    (binding [xss/*xapi-version* xapi-version]
      (xs/validate-statement-data raw-json))
    nil
    ;; JSON errors handled by editor directly, ignore
    (catch js/SyntaxError _)
    ;; Other exceptions will be spec errors
    (catch js/Error e
      (reduce
       (fn [agg {:keys [val path]}]
         (cond-> agg
           (not
            (and (map? val)
                 (some #(= "$templatePath" (name %)) (keys val))))
           (conj
            (if (= path [])
              ;; Top-level error (usually missing req key)
              {:message "xAPI Validation: Missing or invalid root properties."}
              ;; Errors inside a statement sub-object
              {:message (format "xAPI Validation Error in: %s" path)}))))
       []
       (-> e ex-data :error :cljs.spec.alpha/problems)))))

;; Conversion functions

(defn- vectorize-conditions
  [conditions]
  (reduce-kv
   (fn [acc cond-name condition]
     (conj acc (assoc condition :name (name cond-name))))
   []
   conditions))

(defn- mapify-conditions
  [conditions]
  (reduce
   (fn [m {cond-name :name :as condition}]
     (assoc m (keyword cond-name) (dissoc condition :name)))
   {}
   conditions))

(defn db->focus-form
  "Convert `reaction` from how it is stored in the LRS backend into its
   UI view form."
  [reaction]
  (-> reaction
      (update-in [:ruleset :template] w/stringify-keys)))

(defn focus->edit-form
  "Convert `reaction` from its UI view form to its edit form."
  [reaction]
  (-> reaction
      (update-in [:ruleset :conditions] vectorize-conditions)))

(defn focus->download-form
  "Convert `reaction` from its UI view form to its download form."
  [reaction]
  (-> reaction
      (select-keys [:title :ruleset :active])))

(defn upload->edit-form
  "Convert `reaction` from an upload form to its edit form."
  [reaction]
  (-> reaction
      (select-keys [:title :ruleset :active])
      (update-in [:ruleset :template] w/stringify-keys)
      (update-in [:ruleset :conditions] vectorize-conditions)))

(defn edit->db-form
  "Convert `reaction` from its edit form to how it is stored in the
   LRS backend."
  [reaction]
  (-> reaction
      (select-keys [:id :title :active :ruleset])
      (rename-keys {:id :reactionId})
      (update-in [:ruleset :conditions] mapify-conditions)))

;; Path selection dropdown ordering

(def selection-order
  {"id"          0
   "name"        1
   "display"     2
   "definition"  3
   "description" 4})

(defn order-select-entries
  "Order path segment select entries by placing ID and lang map properties at
   the top, then alphabetically for the rest."
  [selects]
  (sort-by :label
           (fn [x y]
             (let [x-order (get selection-order x)
                   y-order (get selection-order y)]
               (cond
                 (and x-order
                      y-order)
                 (< x-order y-order)
                 x-order true
                 y-order false
                 :else   (< x y))))
           selects))
