(defproject parser "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ont-app/igraph "0.1.4-SNAPSHOT"]
                 [clojure-opennlp "0.5.0"] 
                 ]
  :main ^:skip-aot parser.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
