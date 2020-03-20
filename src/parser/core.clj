(ns parser.core
  (:require [clojure.string :as str]
            [ont-app.igraph.core :refer :all]
            [ont-app.igraph.graph :refer :all]
            [ont-app.graph-log.core :as glog]
            [parser.ont :as ont]
            [opennlp.nlp]
            )
  
  (:gen-class))

(def the unique)

(def ontology @ont/ontology-atom)

;; see my notes in 
;; file:analogical-language-model.txt::*Sun%20Sep%2030%20***

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

(defn make-exemplar
  "returns <exemplar> for `category` `term` and `args`
  Where 
  <exemplar> := {<id> <category> <args>}
  <category> is one of #{:S :N :D}, or a combination of same.
  <term> is a token of NL, assocated with <lex>
  <args> := {<arg> <binding>}, (default {?id})
  <arg> is either a keyword corresponding to an associated binding for
    <definition>, or one of #{:id}
  <binding> provides an exemplary binding to <arg>
  <lex> is a lexical entry for the form associated with <term>
  keys(lex) := #{:definition :exemplars ...}
  <definition> is a function associated with <lex>, whose arguments reference
    the <arg>s in <args>
  <id> identifies the exemplar.
  <exemplars> := [<exemplar> ...]
  Note: each <lex> must have at least one exemplar, but will typically have
    many more.
  "
  {:test (fn []
           (assert (= (set (keys (make-exemplar :N "blah")))
                      #{:id}))
           (assert (= (set (keys (make-exemplar
                                  :S
                                  "blah"
                                  {:Ni (make-prototype :cg/Person)})))
                      #{:id :Ni})))
   }
  ([category term]
   (make-exemplar category term {}))
  
  ([category term args]
   {::id (keyword (gensym term))
    :cg/category category
    :cg/args args
    }))




(def metagraph-ref (atom (make-graph :schema {:id ::MetaGraph})))

;;; [[::Metagraph natlex:hasChunk <entryID>...<ExemplarID>
;;;  [<entryID>
;;;    natlex:asGraph <entryChunk>
;;;    natlex:hasExemplar <ExemplarId>
;;;  [<ExemplarId>
;;;   asGraph <ExemplarChunk>]
;;; ]

(defn graph-id [g]
  "Returns <id> for  <g>
Where 
<g> is a graph with schema {:id <id>...}
"
  (:id (.schema g)))

(defn make-chunk [id contents]
  "Returns <chunk> s.t. (:id (:schema 
"
  (add (make-graph :schema {:id id})
       contents))

(defn add-chunk! [chunk]
  "SIDE-EFFECT: `metagraph-ref`, has  `chunk` added.
Where
<metagraph-ref> := [[::Metagraph natlex:hasChunk  <graphName>]
                [<graphName> natlex:asGraph <chunk>]
                ...], an global atom
<chunk> is an instance implmenting IGraph s.t. 
  [:self :id <graphName>]
<graphName> is a keyword naming <chunk>
"
  (reset! metagraph-ref
          (add @metagraph-ref
               [[::MetaGraph :natlex/hasChunk (graph-id chunk)]
                [(graph-id chunk) :natlex/asGraph chunk]
                ])))

#_(defn add-lexicon! []
  "Returns `metagraph-ref` s.t. [[<metagraph> :hasChunk :Lexicon]
                             [:Lexicon :natlex:asGraph <Lexicon>]
                             ...]
                             [[<lexicon> :self :id :Lexicon]
                              [<form> :hasEntry <entry>]
                              [<entry> <p> <o>]]
"
  (add-chunk!
   (add (make-graph
         :schema {:id :Lexicon})
    
    [[:enForm/hello :hasEntry :enLex/hello]
     [:enLex/hello
      :rdf/type :LexicalEntry
      :cg/catLabel "S/N"
      :cg/category :S
      :cg/seekRight :N
      :cg/seekGlobal :D
      :cg/semantics '(fn [{:keys [D N]}]
                       [[:S
                         :sem/eventType :Greeting
                         :sem/addressee N]]
                             

                       )]
     [:enForm/world :hasEntry :enLex/world]
     [:enLex/world
      :rdf/type :LexicalEntry
      :cg/category :N
      :cg/semantics '(fn[]
                       [:N :owl/sameAs :wd/Q16502]
                        )]
      
      ])))

(defn add-lexical-entry!
  "
  SIDE-EFFECT: Metagraph-Ref has an added chunk <entry-id>, whose contents are <entry-spex>
  Where
  <entry-id> is a keyword naming a lexical entry.
  <entry-spex> := [<entry-id> <entry-p> <entry-v>, ...]
  <entry-p> is a property whose domain is a lexical entry
  <entry-v> is a value for <entry-p> appropriate to <entry-id>
  "
  [form entry-spex]
  (let [[entry-id & po] entry-spex]
    (def *po* po)
    (add-chunk!
     (make-chunk entry-id
                 (reduce conj
                         entry-spex
                         [:rdf/type :natlex/LexicalEntry])))))
    
                                   
    
(defn add-lexicon! []
  (add-lexical-entry!
   :enForm/hello 
   [:enLex/hello
    :cg/catLabel "S/N"
    :cg/category [:S]
    :cg/seekRight [:N]
    :cg/seekGlobal [:D]
    :cg/semantics '(fn [{:keys [S N D]}]
                     [[S
                       :sem/eventType :Greeting
                       :sem/addressee N]]
                     )])

  (add-lexical-entry!
   :enForm/world 
   [:enLex/world
    :cg/cat "N"
    :cg/category [:N]
    :cg/semantics '(fn[{:keys [N]}]
                     [[N :owl/sameAs :wd/Q16502]]
                     )]))

(add-lexicon!)

(def interpret identity) ;; for now

(defn canonical-id [&{:keys [type category entry]}]
  (keyword type (str (gensym (reduce str (interpose "-"
                                                    (conj (map name category)
                                                          (name entry))))))))

(defn supplement-args [entry args category-element]
  "Returns `args`, with `category-element` bound to a minted ID
Where
<args> := {<arg> <binding> , ...}
<category-element> is a keyword, typically an element of some category 
  spec in a lexical entry
<arg> is a keyword, typically an 'seek' element of some lexical entry, or
  some category element already bound in a previous call to this function
<binding> is a keyword identifying some entity parsed upstream, or some
  binding to a category-element already produced in a previous call to this 
  function.
"
  (if-not (contains? args category-element)
    #dbg
    (assoc args category-element (canonical-id :type (name category-element)
                                               :category [category-element]
                                               :entry entry))
    ;;else it's already bound in the args...
    args))

(defn annotate-parse 
  "Returns `parse`', annoated for <entry> and <args>
  Where
  <parse> := [[:<s> <p1> <o1> <p2> <o2>]...] generated by application
  of semantics of <entry>
  <parse'> has [<entry> <arg> <binding>...] added for each arg.
  <entry> := the ID of a lexical entry.
  <args> := {<arg> <binding>, ...}
  NOTE: this is done so that the examplar can be matched to future candidates
    for any <arg>
  "
  [entry args parse]
  {:pre [(vector? parse)]
   }
  (letfn [(collect-arg
            [acc k v]
            (conj acc [entry k v]))
          ]
    (reduce-kv collect-arg parse args)))

(defn add-exemplar
  "
  Returns `metagraph-ref`, s.t. an exemplar is added for <entry> using <args>
  Where
  <metagraph> is the metagraph, modified s.t.
    [[<entry> :natlex/hasExemplar <exemplar>]]
    [:Metagraph :natlex/hasChunk <exemplar>]
    [<exemplar> <s> <p> <o>...]
  <entry> names an existing lexical entry
  <args> := {<key> <value>...}, matching the semantics of <entry>
  "
  [entry args]
  (let [entry-chunk (the (@metagraph-ref entry :natlex/asGraph))
        category (the (entry-chunk entry :cg/category))
        cid (canonical-id :type "exemplar"
                          :category category
                          :entry entry)
        args (reduce (partial supplement-args entry)
                     args
                     category)
        exemplar (make-chunk
                  cid
                  (->>
                   (apply (eval (the (entry-chunk entry :cg/semantics)))
                          [args])
                   (annotate-parse entry args)))
        ]
    (add-chunk! exemplar)
    (reset! metagraph-ref
            (add @metagraph-ref 
                 [[entry :natlex/hasExemplar (graph-id exemplar)]]))))
                     

(defn all-lower-case? [token]
  "True iff <token> is all lower-case"
  (= token (str/lower-case token)))

(defn all-upper-case? [token]
  "True iff <token> is all upper-case"
  (= token (str/upper-case token)))

(defn capitalized? [token]
  "True iff <token> is all lower-case"
  (= token (str (str/upper-case (subs token 0 1))
                (str/lower-case (subs token 1)))))

(defn lookup-entry
  "Returns #{<entry>...} for <form>
  Where
  <entry> is a keyword identifying a lexical entry
  <form> is a keyword identifying a lexical form
  "
  {:test (fn [] (not (nil? (lookup-entry :enForm/hello))))
   }
  [form]
  (@metagraph-ref form :hasEntry))


(defn collect-category-pair [weight-fn exemplar target 
                             acc [source-cat target-cat]]
  "returns <acc'> 
Where 
<acc> [<aggregate weight>, {<exemplar> {<exemplar-role> {<analog> <weight>}}]
<aggregate-weight> is the (?)product of each <weight>.
<weight> is the value assiged to the category-pair binding
<exemplar> is part of our reference corpus for say 'hello'
<target> was parsed from the current sentence, for say 'world'
<weight-fn> := (fn [exemplar-role target] -> number)
<source-cat> is a category tag in (category-of source)
<target-cat> is a category tag in (category-of target)
(category-of <exemplar>) -> [:category-tag ...]

Source-cat and target-cat should be mappable to primitive categories :N :S or :D
Mis-matches on these primitives should be severely penalized.
  "
  )

(defn matcher-for 
  "
  Returns (fn [exemplar target] -> 
  -> {<exemplar-role> {<target> <degree-of-match>}}
  Where
  <lexical-entry> := chunk-id s.t. (Metagraph-Ref hasChunk <lexical-entry>
  <seek-arg> is one of #{:cg/seek-left :cg/seek-right}
  <seek-left> and <seek-right> name arguments to ?semantics
  <semantics> is a sequence readable as a (fn [args] ...) -> <parse-graph>
  <target> {:definition <target-definition> :exemplars <target-exemplars>
     :current <current>}, a sup-parse of the current parse
  <exemplar> := {?category ...} is an argument for <seek-arg> in an exemplar
    from <lexical-entry>
  <degree of match> is a number 0...1, reflecting how well <target> matches 
    <source>.
  QUESTION: if we're matching between elements that still have open 'seeks',
    should we combine constraints for when these seeks are resolved downstream?
"

  [lexical-entry seek-arg]
  {:pre [(@metagraph-ref ::Metagraph :natlex/hasChunk lexical-entry)
         (#{:cg/seekRight :cg/seekLeft} seek-arg)
         ]
   
   }
  (let [entry-chunk (the (@metagraph-ref lexical-entry :natlex/asGraph))
        
        ]
    (fn [exemplar target]

      (let [exemplar-chunk (the (@metagraph-ref exemplar :natlex/asGraph))
            source-catagory (seek-arg lexical-entry)
            target-category (category-of target)
            ]
        (assert (= (count (category-of exemplar))
                   (count (catagory-of target))))
        (reduce (partial collect-category-pairing weight-fn exemplar target)
                (map vector source-catagory target-category))))))
        
                          
          
;;(def hello (first (lookup-entry :enForm/hello)))

;; (def hello-exemplar (first (:exemplars (first (lookup-entry test-context "hello")))))
;; (def world (first (lookup-entry test-context "world")))

#_(defn test-matcher-for []
  (apply (matcher-for hello (get-in hello [:definition :seek-right]))
         [(get-in hello-exemplar [:args :N])
          world]))

  
                      
#_(defn parse
  "returns <parsed string>, informed by <context>
  Where
  <instring> is a string of NL text.
  "
  {:test  (fn [] (assert (= (:semantics (parse "Hello World"
                                               test-context))
                            { :s {:rdf/type :greeting
                                  :nl/addressee :world
                                  }})))}
   
  [instring context]
  (let [tokens (tokenize instring)
        entries (map (partial lookup-entry test-context) tokens)
        ]
    entries))

(def english-speaker (add (make-graph :schema {:id :english-speaker})
                          [[:english-speaker :isa :Speaker-Model]]))

(defn substring-id [discourse start end]
  (keyword (name (graph-id discourse))
           (str (str/join "," [start end]))))

(defn new-substring-properties [type discourse contents]
  "Returns <substring-properties>
Where
keys(substring-properties) := #{:rdf/type :nif/referenceContext nif:anchorOf}
"
  {
   :rdf/type type
   :nif/referenceContext (graph-id discourse)
   :nif/anchorOf contents
   })

(def new-sentence-properties
  "Returns new-substring for type :nif/Sentence"
  (partial new-substring-properties :nif/Sentence))

(def new-word-properties
  "Returns new-substring for type :nif/Word"
  (partial new-substring-properties :nif/Word))


(defn kv->po [m]
  "Returns {<p> #{<o>} ,,,} for <m>
Where
<m> := {<p> <o> ...}
Typically used when we have a simple key->single-value map and want to 
add it to the graph
"
  (reduce-kv (fn [acc k v]
               (assoc acc k (set [v]))) {} m))
        
(defn annotate-substring-positions [substrings-property
                                    next-substring-property
                                    parent-id
                                    acc next-substring]
  "Returns [<discourse> <last-offset>] annotated  for <next-substring>
Where
keys(<acc>) := #{:last-offset :sentences}
keys(next-substring) := #{nif:beginIndex, 
                                :nif/endIndex, 
                                :rdf/type}
(<discourse> <parent-id> <substrings-property>)  -> #{[<substring-id>, ...]}
<last-offset> is the last offset of the last sibling substring added.
<substring-property> is one of #{::sentences ::tokens}
<discourse>' := {:self {:id #{<id>}}
                <id> :rdf/type :natlex/Discourse
                <id> :nif/isString <string>
                <id> <substring-property> [<substring-id>, ...]
               }
"
  {:post
   (fn [acc']
     (let [[discourse lo] acc'
           s (last (:sentences discourse))]
       (= (the (discourse s :nif/anchorOf))
          (subs (the (discourse :nif/isString))
                (the (discourse s :nif/beginIndex))
                (the (discourse s :nif/endIndex))))))
   }
  (let [[discourse last-offset] acc
        substrings (or (the (discourse parent-id substrings-property))
                       [])
        previous-substring (last substrings)
        leading-space-count
        (let [[_ spaces]
              (re-find #"^(\s*)"
                       (subs (the
                              (discourse
                               (graph-id discourse)
                               :nif/isString))
                             last-offset))
              ]
          (count spaces))
        beg (+ last-offset leading-space-count)
        end (+ beg (count (:nif/anchorOf next-substring)))
        sid (substring-id discourse beg end)
        ]
    ;; return new acc...
    [(-> discourse
         (subtract [parent-id substrings-property])
         (add (reduce
               conj
               [[parent-id substrings-property (conj substrings sid)]
                [sid :nif/beginIndex beg
                 :nif/endIndex end]
                ]
               (if previous-substring
                 [[previous-substring next-substring-property sid]]
                 [])))
         (add {sid (kv->po next-substring)}))
     ,
     end]))


(def annotate-sentence-positions
  (partial annotate-substring-positions ::sentences :nif/nextSentence))

(def annotate-token-positions
  (partial annotate-substring-positions ::tokens :nif/nextWord))
  

(defn discourse-id [corpus index]
  (keyword corpus (str "D" index)))

(defn new-discourse [corpus index contents]
  (let [discourse-id (discourse-id "mycorpus" index)
        discourse 
        (add (make-graph :schema {:id discourse-id})
             [[discourse-id
              :rdf/type :nif/Context
              :nif/isString contents
              :nif/beginIndex 0
              :nif/endIndex (count contents)
              ]])
        
        sentence-annotated
        (first (reduce (partial annotate-sentence-positions discourse-id)
                       [discourse 0]
                       (map (partial new-sentence-properties discourse)
                            (split-sentences contents))))
        annotate-tokens
        (fn [discourse sentence-id]
          (first (reduce (partial annotate-token-positions sentence-id)
                         [sentence-annotated (the (sentence-annotated
                                                   sentence-id
                                                   :nif/beginIndex))]
                         (map (partial new-word-properties sentence-annotated)
                              (tokenize (the (sentence-annotated
                                              sentence-id
                                              :nif/anchorOf)))))))
        ]
    (glog/value-info
     ::sentence-annotated
     (reduce annotate-tokens sentence-annotated
             (the (sentence-annotated discourse-id ::sentences))))))


(defn -main
  "Demos a parse"
  [& args]
  #_(println (parse "Hello World" {}))
  (let [discourse (new-discourse
                   "mycorpus" 1 "hello world. It's great to be alive.")
        ]
    
    ;; (make-graph :schema {:id (keyword (str (name entry) "#blah"))})
    (let [some-person (keyword (gensym "Person"))
          D (make-graph :schema {:id :enLex/hello#examplar1})
          ]
      (add-exemplar :enLex/hello
                    {:D (add D [some-person :rdf/type :wd/Q5])
                     :N some-person
                     }))))
  

   
