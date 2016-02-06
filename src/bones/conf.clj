(ns bones.conf
  (:require [clojure.string :as s]
            [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [clj-yaml.core :as yaml]
            [clojurewerkz.propertied.properties :as p]
            [clojure.java.io :as io]
            ))

(defn get-extension [file-path]
  (last (s/split file-path #"\.")))

(defmulti parse #'get-extension)

(defmethod parse "edn" [file-path]
  (edn/read-string (slurp file-path)))

(defmethod parse "yml" [file-path]
  (yaml/parse-string (slurp file-path)))

;; both are valid I guess
(defmethod parse "yaml" [file-path]
  (yaml/parse-string (slurp file-path)))

(defmethod parse "properties" [file-path]
  (-> file-path
      (io/file)
      (p/load-from)
      (p/properties->map true)))

(defn quiet-slurp [file-path]
  (try
    (parse file-path)
    (catch IllegalArgumentException e
      (throw (ex-info (str  "File extension not supported: " file-path)
                      {:msg  "add a method to `bones.conf/parse' with a dispatch value matching that extension"})))
    (catch java.io.FileNotFoundException e
      (println (str "WARNING: conf file not found: " file-path)))))

(defn read-conf-data [conf-files]
  (->> conf-files
       (map quiet-slurp)
       (reduce merge {})))

(defn copy-val [acc [k v]]
  (let [important-value (get acc v)]
    (if important-value
      (assoc acc k important-value)
      (throw (ex-info (str "value missing for conf mappy-key: " v)
                      {:conf acc})))))

;; it's unfortunate that reduce will give up and go home if the coll has only one item
(defn copy-values [conf key-set]
  (if (< 1 (count key-set))
    (reduce copy-val conf key-set)
    (let [[k v] (first key-set)]
      (copy-val conf [k v]))))

;; must return a Conf record, not just any map.
(defrecord Conf [conf-files sticky-keys mappy-keys]
  component/Lifecycle
  (start [cmp]
    (let [conf-data (read-conf-data conf-files)]
      (map->Conf (copy-values (merge cmp conf-data) mappy-keys))))
  (stop [cmp]
    ;; keep the specified sticky keys and the special keys themselves
    (map->Conf (select-keys cmp (conj sticky-keys :conf-files :sticky-keys :mappy-keys)))
    ))

(defn start [conf]
  (component/start conf))

(defn stop [conf]
  (component/stop conf))

(defn reload [conf]
  (-> conf
      stop
      start))
