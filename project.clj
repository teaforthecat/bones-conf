(defproject bones.conf "0.1.2"
  :description "application configuration reloaded"
  :url "http://github.com/teaforthecat/bones.conf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.3.1"]]
  :profiles {:test
             {:dependencies [[matcha "0.1.0"]
                             [clj-yaml "0.4.0"]
                             [clojurewerkz/propertied "1.2.0"]]}}


  )
