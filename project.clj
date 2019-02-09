(defproject parser "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ont-app/igraph "0.1.4-SNAPSHOT"]
                 [clojure-opennlp "0.5.0"]
                 [com.taoensso/timbre "4.10.0"]
                 ;; included per docs in slf4j-timbre...
                 [org.slf4j/log4j-over-slf4j "1.7.14"]
                 [org.slf4j/jul-to-slf4j "1.7.14"]
                 [org.slf4j/jcl-over-slf4j "1.7.14"]

                 ]
  :main ^:skip-aot parser.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
