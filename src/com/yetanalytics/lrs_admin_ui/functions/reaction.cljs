(ns com.yetanalytics.lrs-admin-ui.functions.reaction
  (:require [goog.string      :refer [format]]
            [xapi-schema.core :as xs]
            [goog.string.format]))

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

(defn index-conditions
  "Provide sort-indices for reaction conditions editing. Base order on prior
  indices if available."
  [reaction]
  (update-in
   reaction
   [:ruleset :conditions]
   (fn [conditions]
     (reduce
      (fn [m
           [idx
            [condition-key
             condition-val]]]
        (assoc
         m
         condition-key
         (assoc condition-val :sort-idx idx)))
      {}
      (map-indexed
       vector
       (sort-by
        (comp :sort-idx val)
        conditions))))))

(defn strip-condition-indices
  "Remove all indices from conditions."
  [reaction]
  (update-in
   reaction
   [:ruleset :conditions]
   (fn [conditions]
     (reduce-kv
      (fn [m k v]
        (assoc m k (dissoc v :sort-idx)))
      {}
      conditions))))

(defn fix-ruleset-in-path
  "Clojure spec adds an array idx to the :in path on failures of map-of specs.
  This function takes a reaction ruleset condition path and adds the idx,
  making the path usable for finding things."
  [[seg0 seg1 seg2 & rest-seg :as path]]
  (if (and (= [:ruleset :conditions] [seg0 seg1]) seg2)
    (into [seg0 seg1 seg2 1] rest-seg)
    path))

(defn validate-template-xapi
  "Take raw JSON str of an xAPI Statement and issue a vec of error maps for 
   any schema violations. Returns `nil` if `raw-json` is valid."
  [raw-json]
  (try
    (xs/validate-statement-data raw-json)
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
