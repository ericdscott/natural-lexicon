(ns parser.core
  (:require [clojure.string :refer :all]
            [igraph.core :refer :all]
            [igraph.graph :refer :all]
            [opennlp.nlp]
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

(defn sentence-id [discourse start end]
  (keyword (name (id discourse))
           (str (join "," [start end]))))

(defn new-sentence [discourse contents]
  (add (make-graph)
       [[:self
         :rdfs:type :nif:Sentence
         :nif:referenceContext (the (discourse :self :id))
         :nif:anchorOf contents
         ]
        ]))
  
        

(defn annotate-sentence-positions [discourse acc next-sentence]
  "Returns <acc> for <next-sentence>, in context of <discourse>
Where
keys(<acc>) := #{:last-offset :sentences}
<next-sentence> := <sentence-string>
<last-offset> is the last offset of the last member of <sentences>
<sentences> := [<sentence>...]
<sentence> := [[:self :nif:beginIndex ... :nif:endIndex ... :nif:nextSentence

"
  {:post
   (fn [acc']
     (let [s (last (:sentences acc'))]
       (ask s :self :nif:anchorOf 
            (subs (the (discourse :self :nif:isString))
                  (the (s :self :nif:beginIndex))
                  (the (s :self :nif:endIndex))))))
   }
  #dbg     
  (let [previous-sentence (last (:sentences acc))
        add-next-link (fn [sentences]
                        (if previous-sentence
                          (conj (vec (butlast sentences))
                                (add previous-sentence
                                     [[:self
                                       :nif:nextSentence
                                       (id next-sentence)]])))
                        sentences)
        sentence-contents (the (next-sentence :self :nif:anchorOf))
        ;; todo: try index-of instead...
        gap (let [[_ spaces]
                  (re-find #"^(\s*)"
                           (subs (the
                                  (discourse :self
                                             :nif:isString))
                                 (or (:last-offset acc) 0)))
                  ]
              (count spaces))
        
        beg (+ (or (:last-offset acc) 0) gap)
        end (+ beg (count sentence-contents))

        
         ]
        {:last-offset end
         :sentences (conj (add-next-link (or (:sentences acc) []))
                          (add next-sentence
                               [[:self
                                 :id (sentence-id discourse beg end)
                                 :nif:beginIndex beg
                                 :nif:endIndex end
                                 ]
                                ]))
         }
       
        ))

(defn discourse-id [corpus index]
  (keyword corpus (str "D" index)))

(defn new-discourse [corpus index contents]
  (let [discourse 
        (add (make-graph)
              [[:self
                :id (discourse-id "mycorpus" index)
                :nif:isString contents
                :nif:beginIndex 0
                :nif:endIndex (count contents)
                ]])]
    (add discourse
         [[:self
           ::sentences (reduce (partial annotate-sentence-positions discourse)
                               []
                               (map (partial new-sentence discourse)
                                    (split-sentences contents)))
         
           ]])))

                                 
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  #_(println (parse "Hello World" {}))
  (new-discourse "mycorpus" 1 "hello world"))

   
