(ns bones.conf
  (:require [clojure.string :as s]
            [com.stuartsierra.component :as component]
            [clojure.edn :as edn]))

(defn get-extension [file-path]
  (last (s/split file-path #"\.")))

(defmulti parse #'get-extension)

(defmethod parse "edn" [file-path]
  (edn/read-string (slurp file-path)))

(defn quiet-slurp [file-path]
  (try
    (parse file-path)
    (catch IllegalArgumentException e
      (throw (ex-info (str  "File extension not supported: " file-path)
                      {:msg  "add a method to `bones.conf/parse' with a dispatch value matching that extension"})))
    (catch java.io.FileNotFoundException e
      (println (str "WARNING: conf file not found: " file-path)))))

(defn parse-vars [file-path]
  (map #(s/replace % "$" "")
       (re-seq #"\$[A-Z_]*" file-path)))

(defn substitute-env [env]
  (fn [file-path]
    (let [found (parse-vars file-path)]
      (if-not (empty? found)
        ;; TODO: throw if not found in environment?
        ;; $VAR -> ?VAR if not in environment
        (let [getter #(get env % (str "?" %))
              ;; put the $VAR back for the replacement search
              found-pairs (map #(list (str "$" %) (getter %)) found)]
          (reduce (partial apply s/replace ) file-path found-pairs))
        file-path))))

(defn merge-maps [a b]
  (if (and (map? a) (map? b))
    (merge a b)
    ; else, overwrites
    b))

(defn read-conf-data [conf-files]
  (->> conf-files
       (map (substitute-env (System/getenv)))
       (map quiet-slurp)
       (reduce (partial merge-with merge-maps) {})))

(defn copy-val [acc [k v]]
  (let [important-value (get acc v)]
    (if important-value
      (assoc acc k important-value)
      (throw (ex-info (str "value missing for conf mappy-key: " v)
                      {:conf acc})))))

;; it's unfortunate that reduce will give up and go home if the coll has only one item
(defn copy-values [conf key-set]
  (let [n (count key-set)]
    (cond
      (> 1 n)
      (reduce copy-val conf key-set)
      (= 1)
      (let [[k v] (first key-set)]
        (copy-val conf [k v]))
      (= 0 n)
      conf ;;do nothing
      )))

;; must return a Conf record, not just any map.
(defrecord Conf [conf-files sticky-keys mappy-keys]
  component/Lifecycle
  (start [cmp]
    (let [conf-data (read-conf-data conf-files)]
      (map->Conf (copy-values (merge-with merge-maps cmp conf-data) mappy-keys))))
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

;; big data
(defmethod clojure.core/print-method Conf
  [system ^java.io.Writer writer]
  (.write writer "#<bones.conf/Conf>"))
