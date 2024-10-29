(ns com.yetanalytics.lrs-admin-ui.handlers.reaction
  (:require [re-frame.core   :as re-frame]
            [ajax.core       :as ajax]
            [cljs.spec.alpha :refer [valid?]]
            [goog.string     :refer [format]]
            [goog.string.format]
            [day8.re-frame.http-fx]
            [com.yetanalytics.lrs-admin-ui.db                 :as db]
            [com.yetanalytics.lrs-admin-ui.functions          :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.http     :as httpfn]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-admin-ui.spec.reaction-edit :as rse]
            [com.yetanalytics.lrs-reactions.path              :as rpath]
            [com.yetanalytics.lrs-admin-ui.handlers.util      :refer [global-interceptors
                                                                      page-fx]]))

(defmethod page-fx :reactions [_]
  [[:dispatch [:reaction/back-to-list]]
   [:dispatch [:reaction/load-reactions]]])

(re-frame/reg-event-fx
 :reaction/load-reactions
 global-interceptors
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} _]
   {:http-xhrio {:method          :get
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/reaction"
                                   proxy-path)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:reaction/set-reactions]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-db
 :reaction/set-reactions
 global-interceptors
 (fn [db [_ {:keys [reactions]}]]
   (assoc db
          ::db/reactions
          (mapv rfns/db->focus-form reactions))))

(re-frame/reg-event-db
 :reaction/set-focus
 global-interceptors
 (fn [db [_ reaction-id]]
   (assoc db ::db/reaction-focus reaction-id)))

(re-frame/reg-event-db
 :reaction/unset-focus
 global-interceptors
 (fn [db _]
   (dissoc db ::db/reaction-focus)))

(defn- prep-edit-reaction-template
  [{reaction ::db/editing-reaction
    :as      db}]
  (assoc db
         ::db/editing-reaction-template-json
         (.stringify js/JSON
                     (clj->js (get-in reaction [:ruleset :template]))
                     nil
                     2)))

(defn- find-reaction
  [db reaction-id]
  (some
   (fn [{:keys [id] :as reaction}]
     (when (= id reaction-id)
       reaction))
   (::db/reactions db)))

;; TODO: Currently unimplemented
#_(re-frame/reg-event-fx
   :reaction/download-all
   global-interceptors
   (fn [{:keys [db]}]
     (let [reactions (rfns/focus->download-form (::db/reactions db))]
       {:download-edn [reactions "reactions"]})))

(re-frame/reg-event-fx
 :reaction/download
 global-interceptors
 (fn [{:keys [db]} [_ reaction-id]]
   (if-let [reaction (some-> (find-reaction db reaction-id)
                             rfns/focus->download-form)]
     {:download-edn [reaction (:title reaction)]}
     {:fx [[:dispatch [:notification/notify true
                       "Cannot download, reaction not found!"]]]})))

(re-frame/reg-event-fx
 :reaction/upload
 global-interceptors
 (fn [{:keys [db]}]
   {:db db}))

(re-frame/reg-event-fx
 :reaction/edit
 global-interceptors
 (fn [{:keys [db]} [_ reaction-id]]
   (if-let [reaction (some-> (find-reaction db reaction-id)
                             rfns/focus->edit-form)]
     {:db (-> db
              (assoc ::db/editing-reaction reaction)
              prep-edit-reaction-template)
      ;; unset focus in case we're looking at one
      :fx [[:dispatch [:reaction/unset-focus]]]}
     {:fx [[:dispatch [:notification/notify true
                       "Cannot edit, reaction not found!"]]]})))

(re-frame/reg-event-fx
 :reaction/new
 global-interceptors
 (fn [{:keys [db]} _]
   (let [reaction {:title  (format "reaction_%s"
                                   (fns/rand-alpha-str 8))
                   :active true
                   :ruleset
                   {:identityPaths [["actor" "mbox"]
                                    ["actor" "openid"]
                                    ["actor" "mbox_sha1sum"]
                                    ["actor" "account" "homePage"]
                                    ["actor" "account" "name"]]
                    :conditions    []
                    :template      {"actor"
                                    {"name" "Actor Example",
                                     "mbox" "mailto:actor_example@yetanalytics.com"},
                                    "object" {"id" "https://www.yetanalytics.com/xapi/activities/example_activity"},
                                    "verb" {"id" "https://adlnet.gov/expapi/verbs/completed"}}}}]
     {:db (-> db
              (assoc ::db/editing-reaction reaction)
              prep-edit-reaction-template)
      ;; unset focus in case we're looking at one
      :fx [[:dispatch [:reaction/unset-focus]]]})))

(re-frame/reg-event-fx
 :reaction/upload-edit
 global-interceptors
 (fn [{:keys [db]} [_ upload-data]]
   (if-some [edn-data (try
                        (js->clj (js/JSON.parse upload-data)
                                 :keywordize-keys true)
                        (catch js/Error _ nil))]
     (let [reaction (-> edn-data
                        rfns/upload->edit-form)]
       (if (valid? ::rse/reaction reaction)
         {:db (-> db
                  (assoc ::db/editing-reaction reaction)
                  prep-edit-reaction-template)
          :fx [[:dispatch [:reaction/unset-focus]]]}
         {:fx [[:dispatch [:notification/notify true
                           "Cannot upload invalid reaction"]]]}))
     {:fx [[:dispatch [:notification/notify true
                       "Cannot upload invalid JSON data as reaction"]]]})))

(re-frame/reg-event-fx
 :reaction/revert-edit
 global-interceptors
 (fn [{:keys [db]} _]
   (when-let [reaction-id (get-in db [::db/editing-reaction :id])]
     (when-let [reaction (some-> (find-reaction db reaction-id)
                                 rfns/focus->edit-form)]
       {:db (-> db
                (assoc ::db/editing-reaction reaction)
                prep-edit-reaction-template)
        ;; unset focus in case we're looking at one
        :fx [[:dispatch [:reaction/unset-focus]]]}))))

(re-frame/reg-event-fx
 :reaction/server-error
 global-interceptors
 (fn [_ [_ {:keys [response status]}]]
   (if (= 400 status)
     {:fx [[:dispatch
            [:notification/notify true
             (format "Cannot save reaction: %s"
                     (get response :error))]]]}
     {:fx [[:dispatch [:server-error]]]})))

(re-frame/reg-event-fx
 :reaction/save-edit
 global-interceptors
 (fn [{{server-host       ::db/server-host
        proxy-path        ::db/proxy-path
        ?editing-reaction ::db/editing-reaction} :db} _]
   (when-let [{?reaction-id :id
               :as reaction} ?editing-reaction]
     (if (valid? :validation/reaction reaction)
       {:http-xhrio {:method          (if ?reaction-id :put :post)
                     :uri             (httpfn/serv-uri
                                       server-host
                                       "/admin/reaction"
                                       proxy-path)
                     :format          (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :params          (rfns/edit->db-form reaction)
                     :on-success      [:reaction/save-edit-success]
                     :on-failure      [:reaction/server-error]
                     :interceptors    [httpfn/add-jwt-interceptor]}}
       {:fx [[:dispatch
              [:notification/notify true
               "Cannot save invalid reaction."]]]}))))

(defn- cancel-edit
  [db]
  (dissoc db
          ::db/editing-reaction
          ::db/editing-reaction-template-json
          ::db/editing-reaction-template-errors))

(re-frame/reg-event-fx
 :reaction/save-edit-success
 global-interceptors
 (fn [{:keys [db]} [_ {:keys [reactionId]}]]
   {:db (-> db
            cancel-edit
            (assoc ::db/reaction-focus reactionId))
    :fx [[:dispatch [:reaction/load-reactions]]]}))

;; TODO: :reaction/save-edit-fail

(re-frame/reg-event-fx
 :reaction/delete-confirm
 global-interceptors
 (fn [{:keys [db]} [_ reaction-id]]
   (let [{:keys [title]} (find-reaction db reaction-id)]
     {:fx [[:dispatch
            [:dialog/present
             {:prompt (format "Really delete reaction: %s?" title)
              :choices
              [{:label "Cancel"
                :dispatch [:dialog/cancel]}
               {:label "Delete"
                :dispatch [:reaction/delete reaction-id]}]}]]]})))

(re-frame/reg-event-fx
 :reaction/delete
 global-interceptors
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path} :db} [_ reaction-id]]
   {:http-xhrio {:method          :delete
                 :uri             (httpfn/serv-uri
                                   server-host
                                   "/admin/reaction"
                                   proxy-path)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          {:reactionId reaction-id}
                 :on-success      [:reaction/delete-success]
                 :on-failure      [:server-error]
                 :interceptors    [httpfn/add-jwt-interceptor]}}))

(re-frame/reg-event-fx
 :reaction/delete-success
 global-interceptors
 (fn [_ _]
   {:fx [[:dispatch [:reaction/load-reactions]]
         [:dispatch [:reaction/back-to-list]]
         [:dispatch [:notification/notify false
                     "Reaction Deleted"]]]}))

(re-frame/reg-event-db
 :reaction/cancel-edit
 global-interceptors
 (fn [db _]
   (cancel-edit db)))

(re-frame/reg-event-fx
 :reaction/back-to-list
 global-interceptors
 (fn [_ _]
   ;; TODO: Whatever new needs to clear
   {:fx [[:dispatch [:reaction/unset-focus]]
         [:dispatch [:reaction/cancel-edit]]]}))

(re-frame/reg-event-db
 :reaction/edit-title
 global-interceptors
 (fn [db [_ title]]
   (assoc-in db [::db/editing-reaction :title] title)))

(re-frame/reg-event-db
 :reaction/edit-status
 global-interceptors
 (fn [db [_ select-result]]
   (assoc-in db [::db/editing-reaction :active]
             (case select-result
               "active" true
               "inactive" false))))

(defn- remove-element
  [v idx]
  (into (subvec v 0 idx)
        (subvec v (inc idx))))

(re-frame/reg-event-db
 :reaction/delete-identity-path
 global-interceptors
 (fn [db [_ idx]]
   (update-in db
              [::db/editing-reaction :ruleset :identityPaths]
              remove-element
              idx)))

(re-frame/reg-event-db
 :reaction/add-identity-path
 global-interceptors
 (fn [db _]
   (update-in db
              [::db/editing-reaction :ruleset :identityPaths]
              conj
              [""])))

(defn- init-type
  [new-type]
  (case (name new-type)
    "string" ""
    "json"   ""
    "number" 0
    "boolean" true
    "null" nil))

(defn- val-type
  [val]
  (cond
    (string? val) "string"
    (number? val) "number"
    (boolean? val) "boolean"
    (nil? val) "null"))

(defn- ensure-val-type
  "If the path calls for a different type, initialize value."
  [{:keys [path
           val
           ref] :as c}]
  (if ref
    c
    (let [{:keys [leaf-type]} (rpath/analyze-path path)]
      (if leaf-type
        (let [vtype (val-type val)]
          (assoc c :val (if (= (str leaf-type) vtype)
                          val
                          (init-type leaf-type))))
        c))))

(re-frame/reg-event-db
 :reaction/add-path-segment
 global-interceptors
 (fn [db [_ path-path]]
   (let [full-path (into [::db/editing-reaction]
                         path-path)
         path-before (get-in db full-path)
         {:keys [next-keys]} (rpath/analyze-path
                              path-before)
         parent-path (butlast full-path)]
     (-> db
         (update-in full-path
                    conj
                    (if (= '[idx] next-keys) 0 ""))
         (update-in
          parent-path
          ensure-val-type)))))

(re-frame/reg-event-db
 :reaction/del-path-segment
 global-interceptors
 (fn [db [_ path-path]]
   (let [full-path (into [::db/editing-reaction]
                         path-path)
         path-before (get-in db full-path)
         path-after (vec (butlast path-before))
         parent-path (butlast full-path)]
     (-> db
         (assoc-in full-path path-after)
         (update-in
          parent-path
          ensure-val-type)))))

(re-frame/reg-event-fx
 :reaction/change-path-segment
 global-interceptors
 (fn [{:keys [db]} [_ path-path new-seg-val open-next?]]
   (let [full-path           (into [::db/editing-reaction]
                                   path-path)
         path-before         (get-in db full-path)
         path-after          (conj (vec (butlast path-before))
                                   new-seg-val)
         {:keys [leaf-type
                 complete?]} (rpath/analyze-path path-after)
         parent-path         (butlast full-path)]
     (cond-> {:db (-> db
                      (assoc-in full-path path-after)
                      (update-in
                       parent-path
                       ensure-val-type))}
       (and (not complete?)
            (not= 'json leaf-type) ; not extension
            open-next?)
       (assoc :fx [[:dispatch [:reaction/add-path-segment path-path]]])))))

(re-frame/reg-event-db
 :reaction/set-op
 global-interceptors
 (fn [db [_ op-path new-op]]
   (let [full-path (into [::db/editing-reaction]
                         op-path)]
     (assoc-in db full-path new-op))))

(re-frame/reg-event-db
 :reaction/set-val-type
 global-interceptors
 (fn [db [_ val-path new-type]]
   (let [full-path (into [::db/editing-reaction]
                         val-path)]
     (assoc-in db full-path
               (init-type new-type)))))

(re-frame/reg-event-db
 :reaction/set-val
 global-interceptors
 (fn [db [_ val-path new-val]]
   (let [full-path (into [::db/editing-reaction]
                         val-path)]
     (assoc-in db full-path new-val))))

(re-frame/reg-event-db
 :reaction/set-ref-condition
 global-interceptors
 (fn [db [_ condition-path new-condition]]
   (let [full-path (into [::db/editing-reaction]
                         condition-path)]
     (assoc-in db full-path new-condition))))

(re-frame/reg-event-db
 :reaction/set-val-or-ref
 global-interceptors
 (fn [db [_ clause-path set-to]]
   (let [full-path (into [::db/editing-reaction]
                         clause-path)
         reaction (::db/editing-reaction db)
         condition-names (-> reaction
                             :ruleset
                             :conditions
                             (->> (map :name)))
         {:keys [path] :as clause} (get-in db full-path)
         {:keys [leaf-type]} (rpath/analyze-path path)]
     (case set-to
       "val"
       (assoc-in db
                 full-path
                 (-> clause
                     (dissoc :ref)
                     (assoc :val
                            (cond
                              (nil? leaf-type) "" ;; FIXME: This might not work
                              (= 'json leaf-type) "" ;; or this
                              :else (init-type leaf-type)))))
       "ref"
       (assoc-in db
                 full-path
                 (-> clause
                     (dissoc :val)
                     (assoc :ref {:condition (first condition-names)
                                  :path []})))))))

(re-frame/reg-event-db
 :reaction/set-clause-type
 global-interceptors
 (fn [db [_ clause-path clause-type]] ;; #{"and" "or" "not" "logic"}
   (let [full-path (into [::db/editing-reaction]
                         clause-path)
         {?cond-name  :name
          and-clauses :and
          or-clauses  :or
          :as         _clause} (get-in db full-path)
         new-clause
         (cond-> (case clause-type
                   "and"   {:and (or or-clauses [])}
                   "or"    {:or (or and-clauses [])}
                   "not"   {:not nil}
                   "logic" {:path [""]
                            :op   "eq"
                            :val  ""})
           ?cond-name
           (assoc :name ?cond-name))]
     (assoc-in db full-path new-clause))))

(re-frame/reg-event-db
 :reaction/set-condition-name
 global-interceptors
 (fn [db [_ cond-path new-name]]
   (let [full-path (into [::db/editing-reaction]
                         cond-path)]
     (assoc-in db full-path new-name))))

(re-frame/reg-event-db
 :reaction/delete-clause
 global-interceptors
 (fn [db [_ clause-path]]
   (let [full-path (into [::db/editing-reaction]
                         clause-path)
         parent-path (butlast full-path)
         k (last full-path)]
     (cond
       (#{:and :or} (last parent-path))
       (let [parent (get-in db parent-path)
             new-parent (remove-element parent k)]
         (assoc-in db parent-path new-parent))
       (#{:not} (last full-path))
       (assoc-in db full-path nil)
       :else
       (update-in db full-path select-keys [:name])))))

(re-frame/reg-event-db
 :reaction/add-condition
 global-interceptors
 (fn [db [_ ?condition-key]]
   (let [cond-name (or ?condition-key
                       (format "condition_%s"
                               (fns/rand-alpha-str 8)))]
     (update-in
      db
      [::db/editing-reaction :ruleset :conditions]
      conj
      {:name cond-name
       :path [""]
       :op   "eq"
       :val  ""}))))

(re-frame/reg-event-db
 :reaction/delete-condition
 global-interceptors
 (fn [db [_ condition-idx]]
   (update-in
    db
    [::db/editing-reaction :ruleset :conditions]
    remove-element
    condition-idx)))

(re-frame/reg-event-db
 :reaction/add-clause
 global-interceptors
 (fn [db [_ parent-path clause-type]] ;; :and, :or, :not, :logic
   (let [pkey (last parent-path) ;; :and, :or, :not, <condition name>
         full-path (into [::db/editing-reaction]
                         parent-path)
         ?cond-name (get-in db (conj full-path :name))
         new-clause (cond-> (case clause-type
                              :and   {:and []}
                              :or    {:or []}
                              :not   {:not nil}
                              :logic {:path [""]
                                      :op   "eq"
                                      :val  ""})
                      ?cond-name
                      (assoc :name ?cond-name))]
     (if (contains? #{:and :or} pkey)
       (update-in db full-path conj new-clause)
       (assoc-in db full-path new-clause)))))

(re-frame/reg-event-db
 :reaction/update-template
 global-interceptors
 (fn [db [_ new-value]]
   (-> db
       (dissoc ::db/editing-reaction-template-errors)
       (assoc-in [::db/editing-reaction :ruleset :template] new-value))))

(re-frame/reg-event-db
 :reaction/set-template-errors
 global-interceptors
 (fn [db [_ errors]]
   (assoc db ::db/editing-reaction-template-errors errors)))

(re-frame/reg-event-db
 :reaction/clear-template-errors
 global-interceptors
 (fn [db _]
   (dissoc db ::db/editing-reaction-template-errors)))

(re-frame/reg-event-fx
 :reaction/set-template-json
 global-interceptors
 (fn [{:keys [db]} [_ json]]
   (let [xapi-errors (rfns/validate-template-xapi json)]
     (cond-> {:db (assoc db ::db/editing-reaction-template-json json)}
       (seq xapi-errors)
       (assoc :fx [[:dispatch [:reaction/set-template-errors xapi-errors]]])))))

(re-frame/reg-event-db
 :reaction/clear-template-json
 global-interceptors
 (fn [db _]
   (dissoc db ::db/editing-reaction-template-json)))
