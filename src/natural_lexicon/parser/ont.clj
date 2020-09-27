(ns natural-lexicon.parser.ont
  {
   :vann/preferredNamespacePrefix "natlex"
   :vann/preferredNamespaceUri "http://rdf.naturallexicon.org/parser#"
   :rdfs/comment "A parser for NL text"
   }
  (:require
   [clojure.string :as str]
   ;;
   ;;
   [ont-app.vocabulary.core :as voc]
   [ont-app.igraph.core :as igraph
    :refer [add
            ]]
   [ont-app.igraph.graph :as g
    :refer [make-graph
            ]]

   ))

(voc/put-ns-meta!
 'natural-lexicon.category
  {
   :dc/description "Category specifications derived from primitives N, S, or G"
   :vann/preferredNamespacePrefix "cat"
   :vann/preferredNamespaceUri
   "http://rdf.naturallexicon.org/category/"
   })

(voc/put-ns-meta!
 'natural-lexicon.en.lexical-entry
  {
   :dc/description "A container for English word forms"
   :vann/preferredNamespacePrefix "enLex"
   :vann/preferredNamespaceUri
   "http://rdf.naturallexicon.org/en/entry/"
   })



(def ontology-atom (atom (make-graph)))

(defn update-ontology [to-add]
  (swap! ontology-atom add to-add))

(update-ontology
 [
  ;; META
  [:cg/rangeVector
   :rdfs/comment
   "<x> rangeVector <v> Asserts that <v> is a vector, and would need
special processing to render as RDF."
   ]
  [:cg/Vector
   :rdfs/comment "Refers to a construct rendered as a vector"
   ]

  ;; CHUNKS
  [:natlex/Chunk
   :rdf/type :rdfs/Class
   :rdfs/comment
   "Refers to a graph dedicated to representing a chunk whose elements
   are activated together. A Chunk will appear in the metagraph and as
   the :Id of its associated graph."
   ]
  [:natlex/hasChunk
   :rdfs/domain :natlex/CognitiveModel
   :rdfs/range :natlex/Chunk
   :rdfs/comment "Asserts that <chunk> is a URI associated with its own graph."
   ]
  [:natlex/asGraph
   :rdfs/comment "Asserts that <chunk id> refers to the graph <chunk>"
   :rdfs/domain :natlex/Chunk
   :rdfs/range :natlex/ChunkGraph
   ]

  ;; DISCOURSE

  [:cg/Discourse
   :rdf/type :rdf/Class
   :rdfs/comment "Refers to a unit of discourse"
   ]
  [:cg/addressee
   :rdf/type :rdf/Property
   :rdfs/domain :cg/Discourse
   :rdfs/range :cg/Audience
   :rdfs/comment "<discourse> cd/addressee <audience>
Asserts that the content of <discourse> is being directed to <audience> 
Where
<discourse> is a unit of discourse
<audience> is one or more listeners
"
   ]
  [:cg/Audience
   :rdf/type :rdf/Class
   :rdfs/comment
   "Refers to one or more people presumed to share the same model of
   listening fluency in the language of some discourse."
   ]
  ;; OBJECTS IN THE WORLD
  [:cg/Person
   :rdfs/sameAs :wd/Q5
   ]
  
  ;; GRAMMAR
  [:natlex/LexicalEntry
   :rdfs/subClassOf :natlex/Chunk
   :rdfs/comment "Refers to a Lexical entry chunk for one or more related forms."
   ]
  [:cg/cat
   :rdfs/domain :natlex/LexicalEntry
   :rdfs/range :rdf/Literal
   :rdfs/comment "Asserts the CG category for <entry> as a string."
   ]
  [:cg/category
   :rdfs/subPropertyOf :cg/lexicalProperty
   :rdfs/domain :cg/LexicalEntry
   :cg/rangeVector :cg/Category
   :rdfs/comment "<entry> category <category>
Asserts that <entry> when construed in text makes reference to elements of 
<category>, in order of salience.
Where
<entry> is a lexical entry
<category> := [<profile> <oblique-reference> ...]
"
   ]
  [:cg/seekRight
   :rdfs/domain :natlex/LexicalEntry
   :rdfs/rangeVector :cg/Category
   :rdfs/comment "Asserts that <entry> seeks right for the specified category"
   ]
  [:cg/seekLeft
   :rdfs/domain :natlex/LexicalEntry
   :rdfs/rangeVector :cg/Category
   :rdfs/comment "Asserts that <entry> seeks left for the specified category"
   ]
  [:cg/seekGlobal
   :rdfs/domain :natlex/LexicalEntry
   :rdfs/rangeVector :cg/Category
   :rdfs/comment
   "Asserts that <entry> seeks for the specified category without a
   specified direction."
   ]
  [:cg/semantics
   :rdfs/domain :natlex/LexicalEntry
   :rdfs/range :cg/SemanticSpec
   :rdfs/comment
   "Asserts that the specified semantic spec can be applied to a key
   map and return a graph representing the resulting meaning of the
   expression."
   ]
  [:cg/SemanticSpec
   :rdfs/comment
   "A quoted s-expression of the form (fn [{:keys[...]}]...) Returning
   a add-spec for and instance of IGraph. It should specify values for
   each element of :cg/category for the same entry."
   ]
  [:cd/hasExemplar
   :rdfs/domain :natlex/LexicalEntry
   :rdfs/range :natlex/Expression
   :rdfs/comment
   "A relation that appears within the Metagraph applying between the
   Lexical entry and one of its examplars."

   ]
  [:natlex/Expression
   :rdf/subClassOf :natlex/Chunk
   :rdfs/comment "Refers to a representation"
   ]
  
  ]
 )


