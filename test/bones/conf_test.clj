(ns bones.conf-test
  (:require [clojure.test :refer :all]
            [bones.conf :as conf]
            [matcha :as m]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [clojurewerkz.propertied.properties :as p]))


;; test extension
(defmethod bones.conf/parse "yml" [file-path]
  (yaml/parse-string (slurp file-path)))

;; test extension
(defmethod bones.conf/parse "properties" [file-path]
  (-> file-path
      (io/file)
      (p/load-from)
      (p/properties->map true)))


(deftest loading-files
  (testing "slurps a file"
    (let [result (conf/quiet-slurp "test/fixtures/a.edn")]
      (m/is m/map? result)))
  (testing "ignores (and warns) when file doesn't exist"
    (let [result (with-out-str (conf/quiet-slurp "nothing.edn"))]
      (m/is m/string? result)
      (m/is (m/starts-with "WARNING:") result)))
  (testing "throws an exception if file exention is not supported by `bones.conf/parse'"
    (try
      (conf/quiet-slurp "nothing.nosupport")
      (catch Exception e
        (m/is (m/= "File extension not supported: nothing.nosupport")
              (.getMessage e))
        (m/is m/string? (:msg (ex-data e)))))))

(deftest parsing-files
  (testing "reads a edn file"
    (let [result (conf/quiet-slurp "test/fixtures/a.edn")]
      (m/is m/map? result)))
  (testing "reads a yaml file"
    (let [result (conf/quiet-slurp "test/fixtures/a.yml")]
      (m/is m/map? result)))
  (testing "reads a properties file"
    (let [result (conf/quiet-slurp "test/fixtures/a.properties")]
      (m/is m/map? result))))

(deftest merging-values
  (testing "merges files into a single hash"
    ;; the order is important here
   (let [result (conf/read-conf-data ["test/fixtures/a.edn"
                                      "test/fixtures/a.yml"
                                      "test/fixtures/a.properties"])]
     (testing "keeps values from first file"
       (m/is (m/has-entry :abc "xyz") result))
     (testing "overwrites values, last file wins"
       (m/is (m/has-entry :hello "mars") result))
     (testing "merges maps one level deep"
       (m/is (m/has-entry :sub-conf {:something "more-important"
                                     :data "left-over"}) result)))))

(deftest has-component
  (testing "is configurable itself"
    (let [result (conf/map->Conf {:conf-files ["test/fixtures/a.edn"]
                                  :sticky-keys [:abc]
                                  :mappy-keys {:bonjour :hello}
                                  :hello "county"
                                  :sub-conf {:something "less-important"}})
          started (conf/start result)
          stopped (conf/stop started)
          reloaded (conf/reload result)
          ]
      (testing "includes ad-hoc values when started"
        (m/is (m/has-key :sub-conf) started))
      (testing "drops ad-hoc values when reloaded"
        (m/is (m/not (m/has-key :sub-conf)) stopped))
      (testing "holds value of :conf-files :sticky-keys and :mappy-keys"
        (m/is (m/has-entry :conf-files ["test/fixtures/a.edn"]) reloaded)
        (m/is (m/has-entry :sticky-keys [:abc]) reloaded)
        (m/is (m/has-entry :mappy-keys {:bonjour :hello}) reloaded))
      (testing "holds the values read from files that are in :sticky-keys"
        (m/is (m/has-entry :abc "xyz") reloaded))
      (testing "merges maps from files into maps from code"
        (m/is (m/has-entry :sub-conf {:something "important"
                                      :data "left-over"}) reloaded))))
  (testing "does not require mappy-keys"
    (let [result (conf/map->Conf {:conf-files ["test/fixtures/a.edn"]})
          started (conf/start result)
          stopped (conf/stop started)]
      (m/is m/nil? (:mappy-keys started))
      (m/is m/nil? (:mappy-keys stopped))))
  (testing "does not require sticky-keys"
    (let [result (conf/map->Conf {:conf-files ["test/fixtures/a.edn"]})
          started (conf/start result)
          stopped (conf/stop started)]
      (m/is m/nil? (:sticky-keys started))
      (m/is m/nil? (:sticky-keys stopped)))))

(deftest substitute-environment
  (let [result (conf/parse-vars "$WAT-$AB_C")]
    (is (= (list "WAT" "AB_C") result)))
  (testing "environment variables are substituted in the file path"
    (let [file-paths ["abc.edn" "config/$BONES_ENV.edn"]
          result (map (conf/substitute-env {"BONES_ENV" "xyz"}) file-paths)]
      (is (= "abc.edn" (first result)))
      (is (= "config/xyz.edn" (second result))))))
