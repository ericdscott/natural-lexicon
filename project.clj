(defproject parser "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 ;; 3rd party
                 [clojure-opennlp "0.5.0"]
                 ;; ont-app
                 [ont-app/graph-log "0.1.0"]
                 [ont-app/prototypes "0.1.0-SNAPSHOT"]
                 [ont-app/igraph "0.1.4"]
                 [ont-app/igraph-vocabulary "0.1.0-SNAPSHOT"]
                 ]
  :main ^:skip-aot parser.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
