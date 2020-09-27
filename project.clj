(defproject natural-lexicon  "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;; 3rd party
                 [clojure-opennlp "0.5.0"]
                 ;; ont-app
                 [ont-app/igraph-vocabulary "0.1.2-SNAPSHOT"]
                 [ont-app/graph-log  "0.1.1"]
                 [ont-app/prototypes "0.1.0-SNAPSHOT"]
                 [ont-app/igraph "0.1.6-SNAPSHOT"]
                 ]
  :init-ns natural-lexicon.parser.core
  ;; :main ^:skip-aot parser.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
