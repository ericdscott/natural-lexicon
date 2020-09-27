(ns natural-lexicon.parser.core-test
  (:require [clojure.test :refer :all]
            [ont-app.igraph.core :as igraph :refer :all]
            [ont-app.igraph.graph :as native-normal
             :refer [make-graph]]
            [ont-app.igraph-vocabulary.core :as igv
             :refer [mint-kwi]]
            [ont-app.prototypes.core :as proto]
            [ont-app.vocabulary.core :as voc]
            [ont-app.vocabulary.linguistics]
            [ont-app.vocabulary.wikidata]
            [natural-lexicon.parser.core :refer :all]))


(voc/put-ns-meta!
 'natural-lexicon.parser.core-test
 {:vann/preferredNamespacePrefix "test",
  :vann/preferredNamespaceUri "http://rdf.naturallexicon.org/parser/test/"})                  
(reset! metagraph (make-graph))

(define-lexical-entry
  :en
  [:N "person"]
  '(fn [{:keys [D]}]
    (let [N (std-kwi-for D "N")
          ]
      (assert-in D [N
                    :rdf/type :natlex/Entity
                    :owl/sameAs :wd/Q5]))))

(define-lexical-entry
  :en
  [:N "some" :N1]
  '(fn [{:keys [D N1]}]
     (let [N (std-kwi-for D "N")
           ]
       (assert-in D [N :rdf/type N1])))
  [{:N1 :natlex/Thing}])

(deftest test-categories
  (let [g (graph-for :natlex/Categories)
        ]
    (is (= {:N {:natlex/referenceOf
                #{:enLex/N_some+N1
                  :enLex/N_person}}}
           (normal-form g)))))

(deftest test-person
  (let [lex (graph-for :enLex/N_person)
        sem (the (lex :enLex/N_person :natlex/hasSemantics))
        form (graph-for :enForm/person)
        ]
    (is (= [:N :enForm/person]
           (the (lex :enLex/N_person :natlex/hasSyntagm))))
    (is (= {:test/D-person#N
            {:rdf/type #{:natlex/Entity},
             :owl/sameAs #{:wd/Q5}}}
           (let [g (sem {:D :test/D-person})]
             (normal-form (graph-for :test/D-person)))))
    (is (= {:enForm/person
            {:natlex/inSyntagmOf
             #{:enLex/N_person}}})
        (normal-form form))))

(deftest test-some
  (let [lex (graph-for :enLex/N_some+N1)
        sem (the (lex :enLex/N_some+N1 :natlex/hasSemantics))
        form (graph-for :enForm/some)
        ]
    (is (= [:N :enForm/some :N1]
           (the (lex :enLex/N_some+N1 :natlex/hasSyntagm))))
    
    (is (= {:test/D-some#N {:rdf/type #{:test/Dummy}}}
           (let [g (sem {:D :test/D-some :N1 :test/Dummy})]
             (normal-form (graph-for :test/D-some)))))
    
    (is (= {:enForm/some
            {:natlex/inSyntagmOf
             #{:enLex/N_some+N1}}}))
           (normal-form form)))

           

        
           

