(ns com.yetanalytics.lrs-admin-ui.views.util.json
  "JSON display. (For the JSON editor see the `editor` ns.)"
  (:require [reagent.core      :as r]
            ["react-json-view" :as ReactJson]
            [clojure.pprint :refer [pprint]]))

(def json-view-theme
  "Base16 format style input for json viewer, see:
  https://github.com/chriskempson/base16/blob/master/styling.md"
  {:base00 "white"
   :base01 "#ddd"
   :base02 "#ddd"
   :base03 "#444"
   :base04 "#bbb"
   :base05 "#444"
   :base06 "#444"
   :base07 "#444"
   :base08 "#444"
   :base09 "#11375C"
   :base0A "#11375C"
   :base0B "#11375C"
   :base0C "#11375C"
   :base0D "#11375C"
   :base0E "#137BCE"
   :base0F "#11375C"})

(defn json-viewer
  "Viewer for JSON data. Uses `react-json-view` as the underlying view lib,
   which does not permit top-level scalar values unless `allow-scalars?` is
   `true` to override it."
  [{:keys [data name collapsed display-data-types icon-style]
    :or   {name               false
           collapsed          false
           display-data-types false
           icon-style         "square"}}]
  [(r/adapt-react-class (aget ReactJson "default"))
   {:src              data
    :name             name
    :collapsed        collapsed
    :displayDataTypes display-data-types
    :theme            json-view-theme
    :iconStyle        icon-style}])
