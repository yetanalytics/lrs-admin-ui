(ns com.yetanalytics.lrs-admin-ui.functions.scopes)

(def scope-list
  ["all"
   "all/read"
   "activities_profile"
   "agents_profile"
   "state"
   "statements/read"
   "statements/read/mine"
   "statements/write"])

(def statement-read-scope-set
  #{"all"
    "all/read"
    "statements/read"
    "statements/read/mine"})

(defn has-scope?
  "Does `scope-coll` include `scope`?"
  [scope-coll scope]
  (some? (some #{scope} scope-coll)))

(defn toggle-scope
  "Add `scope` to `scope-coll` if it's not included; remove `scope` from
   `scope-coll` if it is."
  [scope-coll scope]
  (if (has-scope? scope-coll scope)
    (remove #(= scope %) scope-coll)
    (conj scope-coll scope)))

(defn has-statement-read-scopes?
  "Does the credential have scopes that can read the LRS?"
  [{:keys [scopes] :as _credential}]
  (some statement-read-scope-set scopes))
