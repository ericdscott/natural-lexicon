(ns natural-lexicon.parser.core
  (:require [clojure.string :as str]
            [ont-app.igraph.core :refer :all]
            [ont-app.igraph.graph :refer :all]
            [ont-app.igraph-vocabulary.core :as igv
             :refer [mint-kwi]]
            [ont-app.graph-log.core :as glog]
            [ont-app.vocabulary.core :as voc]
            [ont-app.vocabulary.linguistics]
            [ont-app.vocabulary.wikidata]
            [natural-lexicon.parser.ont :as ont]
            [opennlp.nlp]
            )
  
  (:gen-class))

(def the unique)

(def ontology @ont/ontology-atom)

(defmethod mint-kwi :natlex/Discourse
  [_ & {:keys [nif/sourceUrl, nif/broaderContext, nif/beginIndex nif/endIndex] :as args}]
  ;; uses either the broader context or the URL, plus offsets.
  (letfn [(str-for [maybe-keyword]
            (if (keyword? maybe-keyword)
              (name maybe-keyword)
              maybe-keyword))
          ]
    (voc/keyword-for
     (str (voc/qname-for :natlex/Discourse)
          "/"
          (voc/qname-for (or broaderContext
                             sourceUrl))
          (if (and beginIndex endIndex)
            (str
             "#"
             (apply str
                    (map str (interpose ","
                                        (map str-for
                                             [beginIndex
                                             endIndex])))))
            "")))))

(defmethod mint-kwi :natlex/EnglishLexicalEntry
  [_ & {:keys [natlex/syntagm] :as args}]
  (voc/keyword-for
   (str (voc/prefix-to-namespace-uri "enLex")
        (voc/encode-uri-string (str (-> syntagm first name)
                                    "_"
                                    (str/join "+"
                                              (map name (rest syntagm))))))))

  

(defmethod mint-kwi :natlex/Exemplar
  [_ & {entry :natlex/lexicalEntry paradigm :natlex/paradigm}]
  (voc/keyword-for (voc/encode-uri-string
                    (str
                     (voc/qname-for :natlex/Exemplar)
                     "/"
                     (name entry)
                     (format "/p%X"
                             (Math/abs (hash paradigm)))))))

;; English tokenizer...
(defonce tokenize (opennlp.nlp/make-tokenizer
                   "http://opennlp.sourceforge.net/models-1.5/en-token.bin"))

(defonce split-sentences  (opennlp.nlp/make-sentence-detector
                           "http://opennlp.sourceforge.net/models-1.5/en-sent.bin"))


(defn make-prototype [class-name]

  "Returns <prototype>, a dummy semantic spec for `class name`
Where:
<class-name> is a keyword naming a class of entities.
(keys <prototype>) := #{:id :instanceOf} + <implicits>
<implicits> := {<key> <value> ...}, implied based on <class-name>, e.g. 
  :Discourse class-name implies that there is an addressee who is a person.
"
  (let [implicits (case class-name
                     :cg/Discourse {:cg/addressee (make-prototype :cg/Person)}
                     {})
        ]
        (merge implicits
               {::id (keyword (gensym class-name))
                :cg/instanceOf class-name})))


(def metagraph (atom (make-graph)))

(defn graph-for
  "Returns the graph associated with `chunk-id` in the metagraph"
  [chunk-id]
  (the (@metagraph chunk-id :igraph/compiledAs)))

(defn assert-in
  "Side-effect: Updates the state of `chunk` with `to-add`
  Where
  `chunk` is the graph refererred to by `chunk-id`, or an new graph.
  `to-add` is a set of triples to add to `chunk`.
  `chunk-id` is a KWI for some chunk in the metagraph"
  
  [chunk-id to-add]
  (let [chunk (or (the (@metagraph chunk-id :igraph/compiledAs))
                  (make-graph))
        ]
    (swap! metagraph
           assert-unique
           chunk-id
           :igraph/compiledAs
           (add chunk to-add))))


#_(defn all-lower-case? [token]
  "True iff <token> is all lower-case"
  (= token (str/lower-case token)))

#_(defn all-upper-case? [token]
  "True iff <token> is all upper-case"
  (= token (str/upper-case token)))

#_(defn capitalized? [token]
  "True iff <token> is all lower-case"
  (= token (str (str/upper-case (subs token 0 1))
                (str/lower-case (subs token 1)))))


(defn form-prefix
  "Returns the qname-prefix for `lang`
  Where
  `lang` is a keyword naming a language tag, e.g. :en"
  [lang]
  (case lang
    :en "enForm"))


(defn eval-semantic-spec
  "Evaluates `spec`
  Where
  `spec` := (quote (fn [<args>])) definiing the semantic component of some lexical
    entry.
  `args` := {:D <discourse KWI>, ...}, corresponding to elements of the syntagm
  "
  [spec]
  (eval spec))
                                     
(defn add-exemplar
  "Side-effect: Adds chunks to the metagraph instantiating an exemplar of `entry-id` with `semantics`, applied to `pardigm`
  Where
  `entry-id` is the kwi associated with some lexical entry
  `semantics` := (fn [<args>]-> metagraph is the semantic component of some lexical
    entry.
  `args` := {:D <discourse KWI>, ...}, corresponding to elements of the syntagm
  "
  [entry-id semantics pardigm]
  (let [exemplar-id (mint-kwi :natlex/Exemplar :natlex/lexicalEntry entry-id :natlex/paradigm (voc/encode-kw-name (str pardigm)))
        g (make-graph)
        entry-graph (graph-for entry-id)
        ]
    (assert-in entry-id [entry-id :natlex/hasExemplar exemplar-id])
    (assert-in exemplar-id [exemplar-id
                            :rdf/type :natlex/Exemplar
                            :natlex/lexicalEntry entry-id
                            :igraph/compiledAs g])
    (apply semantics [(merge {:D exemplar-id} pardigm)])))


(defn std-kwi-for
  "Returns the canonical KWI for `syntagm-elt` in `D`
  Where
  `syntagm-elt` is a member of the syntagm element of some `entry`
  `D` is a KWI referencing the discourse context presumed `entry`
  Note: this is typically used when naming variables in to semantics of `entry`"
  [D syntagm-elt]
  (voc/keyword-for (str (voc/uri-for D)
                        "#"
                        syntagm-elt)))


(defn define-lexical-entry 
  "Side effect: establishes `entry` for `syntagm` in `lang` with `semantics` in [[`metagraph`]
Where
- `entry` identifies a lexical entry in [[`metagraph`]]
- `syntagm` := [`category` & var or form]
"
  ([lang syntagm-spec semantics-spec]
   #dbg
   (when (not (empty? (filter keyword? (rest syntagm-spec))))
     (throw (ex-info (str "Syntagm spec " syntagm-spec " has parameters but not paradigm-spec")
                     {:type :missing-paradigm-spec
                      :lang lang
                      :syntagm-spec syntagm-spec
                      :semantics-spec semantics-spec})))
   (define-lexical-entry lang syntagm-spec semantics-spec [{}]))
  
  ([lang syntagm-spec semantics-spec paradigm-spec]
   (let [syntagm (mapv (fn [elt]
                         (if (string? elt)
                           (voc/keyword-for (str (form-prefix lang) ":" elt))
                           elt))
                       syntagm-spec)
         entry-id (mint-kwi :natlex/EnglishLexicalEntry :natlex/syntagm syntagm)
         
         semantics (eval-semantic-spec semantics-spec)
         ]
     
     (assert-in entry-id [entry-id
                         :natlex/hasSyntagm syntagm
                         :natlex/hasSemantics semantics])
     (assert-in :natlex/Categories
                [(first syntagm) :natlex/referenceOf entry-id])
     
     (doseq [elt (rest syntagm)]
       (cond (#{"enForm"} (namespace elt))
             (assert-in elt [elt :natlex/inSyntagmOf entry-id])))
     
     (map (partial add-exemplar entry-id semantics) paradigm-spec))))

   
#_(defn -main
  "Demos a parse"
  [& _]
  (println (parse "some person" {}))
  (let [discourse (new-discourse
                   "mycorpus" 1 "hello world. It's great to be alive.")
        ]
    
    ;; (make-graph :schema {:id (keyword (str (name entry) "#blah"))})
    (let [some-person (keyword (gensym "Person"))
          D
          ^{::id :enLex/hello#examplar1} (make-graph)
          ]
      (add-exemplar :enLex/hello
                    {:D (add D [some-person :rdf/type :wd/Q5])
                     :N some-person
                     }))))
