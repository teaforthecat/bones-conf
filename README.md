# bones.conf

A Clojure library designed to read configuration files and provide a
component to inject into a component system-map.

## Usage

Recommended implementation

```clojure
(defn system []
  (atom (component/system-map
         :conf (conf/map->Conf :conf-files ["config.edn" "/etc/sysconfig/app.properties"]
                               :port 3000 ;; override-able via conf-files
                               :sticky-keys [:port] ;; and reloadable
                               :mappy-keys [[:zookeeper-addr :zookeeper/address]]) ;;same value
         :http (component/using
                (map->HTTP {}) ;; uses (:port conf) in start
                [:conf]))

```

## Adding a file type

```
(require '[clj-yaml.core :as yaml])

(defmethod bones.conf/parse "yml" [file-path]
  (yaml/parse-string (slurp file-path)))
```


```
(require '[clojurewerkz.propertied.properties :as p])

(defmethod bones.conf/parse "properties" [file-path]
  (-> file-path
      (io/file)
      (p/load-from)
      (p/properties->map true))))
```

## Development and Testing

start repl with `lein with-profile +test repl` and evaluate `testing` forms in `test/conf_test.clj`

command line tests via `lein test`


## License

Copyright © 2016 Chris Thompson

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
