(defproject bones/conf "0.2.4"
  :description "application configuration reloaded"
  :url "http://github.com/teaforthecat/bones-conf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [aero "1.1.2"]]
  :profiles {:test
             {:dependencies [[matcha "0.1.0"]
                             [clj-yaml "0.4.0"]
                             [clojurewerkz/propertied "1.2.0"]]}
             ;; add :test to default so dependencies aren't propagated
             :default [:base :system :user :provided :dev :test]}
  )
