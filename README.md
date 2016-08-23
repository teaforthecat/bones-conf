# bones.conf

A Clojure library designed to read configuration files and provide a
component to inject into a component system-map.


![travis status](https://api.travis-ci.org/teaforthecat/bones.conf.svg)


## Usage

### Recommended implementation

```clojure
(ns your.ns
  (require [bones.conf :as conf]
           [com.stuartsierra.component :as component]))

(def sys (atom {}))

(swap! sys assoc :conf (conf/map->Conf {:conf-files ["test.edn"]}))
(swap! sys assoc :xyz (component/using (map->XYZ {}) [:conf]))

;; conf will be started and assoc'd to xyz before its `start` function is executed

(swap! sys component/update-system [:xyz :conf] component/start)

;; reload conf files
(swap! sys update :conf component/start)
```

### Special keys

Any key can be a configuration variable but these are treated special.


`:conf-files` *required*  A list of file paths that can be absolute or relative.
The files will get read and parsed according to the file extension. Only .edn is
supported out of the box but see below for others.

`:sticky-keys` *optional* Sometimes you want to keep the configuration values
around when you set them on the Conf record it code and reload the component.
The sticky-keys will not get lost unless they also exist in a file.

`:mappy-keys` *silly* Just in case you want to use the same value for multiple
keys. The second one is the real value, the first is the "symlink".



### Alternative implementation:

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
