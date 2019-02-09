(ns parser.core
  (:require [clojure.string :refer :all]
            [igraph.core :refer :all]
            [igraph.graph :refer :all]
            [opennlp.nlp]
            [taoensso.timbre :as log]
            )
  
  (:gen-class))

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
                     :Discourse {:addressee (make-prototype :Person)}
                     {})
        ]
        (merge implicits
               {:id (keyword (gensym class-name))
                :instanceOf class-name})))

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
                                  {:Ni (make-prototype :Person)})))
                      #{:id :Ni})))
   }
  ([category term]
   (make-exemplar category term {}))
  
  ([category term args]
   {:id (keyword (gensym term))
    :category category
    :args args
    }))



(def test-context
  {:lexicon {"hello" [{:definition {:category :S
                                    :seek-right :N
                                    :seek-global :D
                                    :semantics '(fn [{:keys [N D]}]
                                                  {:S {:instanceOf :Greeting
                                                       :addressee N}
                                                   N {:sameAs (:addressee D)}})}
                       :exemplars [(make-exemplar
                                    :S
                                    "hello"
                                    {
                                     :D (merge {:category :D}
                                               (make-prototype :Discourse))
                                     :N (merge {:category :N}
                                               (make-prototype :Person))
                                     })
                                   ]}]
             "world" [{:definition {:category :N
                                    :semantics '(fn []
                                                  {:N {:sameAs :TheWorld}})}
                       :exemplars [(make-exemplar :N "world")]}]}})


     
(defn all-lower-case? [token]
  "True iff <token> is all lower-case"
  (= token (lower-case token)))

(defn all-upper-case? [token]
  "True iff <token> is all upper-case"
  (= token (upper-case token)))

(defn capitalized? [token]
  "True iff <token> is all lower-case"
  (= token (str (upper-case (subs token 0 1))
                (lower-case (subs token 1)))))

(defn lookup-entry
  {:test (fn [] (not (nil? (lookup-entry test-context "hello"))))
   }
  [context token]
  
  ;; TODO: deal with capitalization issues
  (get-in context [:lexicon (lower-case token)]))

    
(defn matcher-for 
  "
  Returns (fn [source target] -> {?source ?target ?degree-of-match} for `lexical-entry` and `arg`
  Where
  <lexical-entry> := {:definition <source-definition> :exemplars <exemplars>}
  <source-defintion> := {?seek-left ?seek-right ?semantics...}
  <arg> is one of #{<seek-left> <seek-right>}
  <seek-left> and <seek-right> name arguments to ?semantics
  <semantics> is a sequence readable as a (fn [args] ...) -> <parse-graph>
  <target> {:definition <target-definition> :exemplars <target-exemplars>
     :current <current>}, a sup-parse of the current parse
  <source> := {?category ...} is an argument for <arg> in an exemplar from
    <lexical-entry>
  <degree of match> is a number 0...1, reflecting how well <target> matches 
    <source>.
"

  [lexical-entry arg]
  {:pre [(map? lexical-entry)
         (:definition lexical-entry)
         (not (nil? arg))
         ]
   
   }
  (fn [source target]
    {:pre [(get-in source [:category])
           (get-in target [:definition :category])
           ]
     }
    (def _source source)
    (def _target target)
    (let [
          match-categories
          (fn [score]
            (if (= (get-in source [:category])
                   (get-in target [:definition :category]))
              (+ score 0.5)
              score))
                           
        ]
    (-> 0
        match-categories))))
          
(def hello (first (lookup-entry test-context "hello")))
(def hello-exemplar (first (:exemplars (first (lookup-entry test-context "hello")))))
(def world (first (lookup-entry test-context "world")))

(defn test-matcher-for []
  (apply (matcher-for hello (get-in hello [:definition :seek-right]))
         [(get-in hello-exemplar [:args :N])
          world]))

  
                      
(defn parse
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

(def english-speaker (add (make-graph)
                          [[:self :id :english-speaker]
                           [:english-speaker :isa :Speaker-Model]
                           ]))


(def the unique)

(defn id [g]
  (the (g :self :id)))

(defn rename-graph [g]
  (let [current-name (or (id g) :self)]
    (query g [[current-name :?p :?o]])))
         

(defn substring-id [discourse start end]
  (keyword (name (id discourse))
           (str (join "," [start end]))))

(defn new-substring-properties [type discourse contents]
  "Returns <substring-properties>
Where
keys(substring-properties) := #{:rdf:type :nif:referenceContext nif:anchorOf}
"
  {
   :rdf:type type
   :nif:referenceContext (the (discourse :self :id))
   :nif:anchorOf contents
   })

(def new-sentence-properties
  "Returns new-substring for type :nif:Sentence"
  (partial new-substring-properties :nif:Sentence))

(def new-word-properties
  "Returns new-substring for type :nif:Word"
  (partial new-substring-properties :nif:Word))


(defn normalize [m]
  "Returns each value in <m> as a set to support graph normal form.
Typically used when we have a simple key->single-value map and want to add it ot the graph
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
                                :nif:endIndex, 
                                :rdf:type}
(<discourse> <parent-id> <substrings-property>)  -> #{[<substring-id>, ...]}
<last-offset> is the last offset of the last sibling substring added.
<substring-property> is one of #{::sentences ::tokens}
<discourse>' := {:self {:id #{<id>}}
                <id> :rdf:type :natlex:Discourse
                <id> :nif:isString <string>
                <id> <substring-property> [<substring-id>, ...]
               }
"
  {:post
   (fn [acc']
     (let [[discourse lo] acc'
           s (last (:sentences discourse))]
       (= (the (discourse s :nif:anchorOf))
          (subs (the (discourse :nif:isString))
                (the (discourse s :nif:beginIndex))
                (the (discourse s :nif:endIndex))))))
   }
  (let [[discourse last-offset] acc
        graph-id (the (discourse :self :id))
        substrings (or (the (discourse parent-id substrings-property))
                       [])
        previous-substring (last substrings)
        leading-space-count
        (let [[_ spaces]
              (re-find #"^(\s*)"
                       (subs (the
                              (discourse graph-id :nif:isString))
                             last-offset))
              ]
          (count spaces))
        beg (+ last-offset leading-space-count)
        end (+ beg (count (:nif:anchorOf next-substring)))
        sid (substring-id discourse beg end)
        ]
    ;; return new acc...
    [(add (add (subtract discourse
                         [parent-id substrings-property])
               (concat 
                [[parent-id substrings-property (conj substrings sid)]
                 [sid :nif:beginIndex beg
                      :nif:endIndex end]
                 ]
                (if previous-substring
                  [[previous-substring next-substring-property sid]]
                  [])))
          {sid (normalize next-substring)})
     end]))

(def annotate-sentence-positions
  (partial annotate-substring-positions ::sentences :nif:nextSentence))

(def annotate-token-positions
  (partial annotate-substring-positions ::tokens :nif:nextWord))
  
                                          


(defn discourse-id [corpus index]
  (keyword corpus (str "D" index)))

(defn new-discourse [corpus index contents]
  #dbg
  (let [discourse-id (discourse-id "mycorpus" index)
        discourse 
        (add (make-graph)
              [[:self
                :id discourse-id]
               [discourse-id
                :rdf:type :nif:Context
                :nif:isString contents
                :nif:beginIndex 0
                :nif:endIndex (count contents)
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
                                                   :nif:beginIndex))]
                         (map (partial new-word-properties sentence-annotated)
                              (tokenize (the (sentence-annotated
                                              sentence-id
                                              :nif:anchorOf)))))))
        ]
    sentence-annotated
    #_(log/info (normal-form sentence-annotated))
    (reduce annotate-tokens sentence-annotated
            (the (sentence-annotated discourse-id ::sentences)))))

                                 
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  #_(println (parse "Hello World" {}))
  (normal-form (new-discourse "mycorpus" 1 "hello world. It's great to be alive.")))

   
