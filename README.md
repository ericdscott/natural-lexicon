# parser

This project aims to implement a construction-based NL parser in clojure.

## Contents
- [Installation](#installation)
- [Basic usage](#basic-usage)
- [The Knowledge Resource (KR)](#the-knowledge-resource-kr)
- [Activation](#activation)
- [Categories](#categories)
  - [Primitive Categories](#primitive-categories)

<a name="h2-installation"></a>
## Installation

Watch this space.

<a name="h2-basic-usage"></a>
## Basic usage

Invoke the parser thus:

```
> (parse <kr> <document>) -> <kr'>

```

Where
- kr := `{<chunk-id> {<asGraph> #{<g>},
                     <p> #{<o>...}
                     ...}}`
- `<p>` reflects things like base-activation of the chunk.
- `<g>` := a graph representng an activation pattern 

The _KR_ will be changed as follows:

- A _chunk_ representing the discourse associated with _document_ will
  be added to _KR_.
- The document discourse chunk will be a graph with the following
  characteristics:
  - Each entity spoken of in the document will be a subject, encoded
    as a KWI with of category 'N'. Each such entitiy in turn will have
    a chunk in the KR.
  - Each relationship spoken of in the document will be a subject
    encoded as a KWI with category 'S'. Each such relationship in turn
    will have a chunk in the KR.
  - Both Ns and Ss will have types, which in turn will be represented as chunks in the KR
  - Each sub-discourse (heading, paragraph, table, etc) in the discourse will be represented as a subject with category 'D', linked to a chunk.
  
<a name="h2-the-knowledge-resource"></a>
## The Knowledge Resource (KR)
The Knowledge Resource or KR is a 'metagraph' which holds your linguistic knowledge, holding a number of specialized graphs with metadata about each graph. 


## Chunks
Many of these graphs are 'chunks', which are immutable graphs
dedicated to representing an 'activation pattern'. Examples of chunks
would be lexical entries, parses and sub-parses, as well as whole
discourses and representations of real-world entities. Each chunk can
be assigned a level of activation reflecting its availability to the
model at any point in the parsing process. Chunks are typically
created by modifying chunks already in the KR at the time they are
created, and because chunks are immutable, there is a large degree of
shared structure between chunks.

Note that each chunk has an external aspect reflected in the KR, and
an internal aspect held in the chunk-graph itself.

### Activation
This is directly inspired by the ACT-R model.

Processing a parse involves maintaining a graph dedicated to
representing a _context_. Processing proceeds by reiteratively
applying a set of production rules keyed to the current context. These
rules will query against a knowledge graph consisting of chunks.

Each chunk is associated with an activation level reflecting its
availability at any time during processing. During processing, chunks
will be queried for in order to fit them to rules keyed to the current
context. Retrieved chunks will be ordered by their degree of
activation.

Chunks which have been active recently will tend to be active, and
that activation will decay over time.

Chunks linked to chunks which are currently in-context will receive 'spreading activation'.

The activation for chunks which are frequently re-activated will tend
to decay more slowly than chunks that are not often activated.

## Categories

This parser is informed by Categorial Grammar to some degree, and each parse and sub-parse is assigned a category constructed as follows:

### Primitive categories
#### Category `D` 
- :D a unit of discourse
  - Each sentence takes place within a tree of D's, whose root D is
    the top-level discourse
  - Properties whose domain is a discourse would include _partOfDiscourse_, _speaker_, _audience_,  _venue_, _time_,  _hasParticipant_, _hasEvent_, _hasState_, ...
  - Note that we can also model the speaker and listener, as well as
    the occasion, such as a political rally, etc.
  - Discourses can be queried for their contents in the course of parsing, 
    - queries may traverse the tree of discourses and sub-discourses.
    
#### Category `N`
- :N a nominal entity
  - Typical of noun phrases
  - Properties would reflect the entities being described
  
#### Category `S`
- :S a sentential relationship
  - Typical of verb phrases and whole sentences
  - Each instance of an S will be associated with a type, which will
    typically be associated with a set of roles.
  - Each role will typically be a sub-property of typical thematic
    roles like _agent_, _patient_, etc.


### Catagory composition

Categories can also be composed out of primitive categories, using
relations that describe how they combine. Some examples:


| category | description | example  |
|---|---|---|
| `[N1:N1]` |  an adjective, _seeking_ an entity, and referencing that same entity, modified. | `([N1:N1] blue)`  |
| `[N1:N2]` | a determiner, _seeking_ a reference to a class description, and referencing <br/> an instance of that class | `([N1:N2] the)`|
| `[S1:N1:N2]` | a transitive verb, _seeking_ a subject reference _N1_ and an object reference _N2_, <br/>returning a reference to a certain event type with N1 and N2 bound to certain roles. | `([S:N1:N2 eats)` |
| `[N1|S1]` | A reified verb, making a _direct reference_, _N1_ to a reification of of a sentential <br> _indirect reference_, _S1_. | `([N1|S1] operation)`|

Complex categories have the following properties:

- Each component of the category corresponds to a _reference_ to some
  N, S, or D.
- A complex category my _seek_ a reference to some other construct
  with which it expects to combine.
- Each category puts its primary reference _in profile_, but may also
  provide secondary references which are not in profile, but may be
  matched to other categories seeking those references.
  
Categories may also be expressed in the context of a _syntagm_, which
expresses expectation of matching behavior within a sequence.

| synagm | abstract syntagm | parent category |
|---|---|---|
|`(N1 blue N1)` |`(N1 _ N1)` | `[N1:N1]`|
|`(N1 the N2)` |`(N1 _ N2)` | `[N1:N2]`|
|`(S1 N1 eats N2)` |`(S1 N1 _ N2)` | `[S1:N1:N2]`|
|`(N1|S1 action)` |`(N1|S1 _)` | `[N1|S1]`|
|`(N1|S1 to S1:N2)` |`(N1|S1 _ S1:N1)` | `[N1|S1:(S1:N2)]`|

Complex categories may also be expressed with glosses:

`(S1-speech-event N1-speaker said " D1-utterance ")`

### Lexical Entries

One kind of chunk is a _LexicalEntry_, anchored at a prototype of the
same name.

```
[[cg/LexicalEntry
  :rdf/type :proto/Prototype
  :proto/hasParameter :cg/syntagm
  :proto/hasParameter :cg/category
  :proto/hasParameter :cg/semantics
  ]]
```
Each _syntagm_ is as described above in the discussion of categories.


When a match element is introduced in an elaboration of a lexical entry, that match element becomes an additional parameter.

Eg:
```
Watch this space.
```
Each _match-spec_ describes a category, expressed as a vector of keywords starting with N S or D. The first of which specifies the primary reference or profile of the expression, with subsequent keywords specifying secondary references. Keywords are treated as variables, and non-co-referential variables will need to have different names.

Kill what follows aftar adapting.

A lexical entry is described thus:

```
(C0|C1:C3... {sem} (C4...) <lex1> (C5 ...) <lex2> (C6 ...) ...)

```
Where
- Each CX refers to a D N or S encounted during parsing.
- C0 identifies the direct referent of the expression
  - The category of C0 determines the expression's _profile_ (Langacker)
- '|' is an operator to specify indirect referents
- C1 is an indirect referent of the expression
  - These referents may also inform the matching process
- ':' is an operator to specify schematic referents
- C3 is a schematic referent, i.e. a component of the resulting
  construction which has yet to be specified.
  - The parsing process will seek to unify with adjoining expressions
    whose profiles match the schematic referents.
- SEM is a function returning a chunk-graph representing the contents of the parse.


Examples

Actually, I don't like this.

How about:

```
  (add (make-chunk)
   [:LexicalEntry_hello-1
    :cg/profile [:?S]
    :cg/syntagm  [:enForm/hello :?N1]
    :cg/semantics '(fn [{:keys [S N D]}]
                     [[S
                       :proto/elborates :Greeting
                       :sem/addressee N]]
                     )
                     
    :cg/seek :?N1 ;; implicit? 
    ]
    [:?S
     :cg/category [:S]
     :paradigm (fn [chunk] ...) -> 0..1
    ]
    [:?N1
     :cg/category [:N]
     :paradigm (fn [chunk] ...) -> 0.1
    ]
    )

```

With derivable category:
```
"(S1 hello N1)"
```

[:Category_S1_syn_true_N1 
 :proto/elaborates :Category_S1_seek_N1
 :syntagm [true :?N1]
]
 
[:Category_S1_seek_N1
 :proto/elaborates :Category_S1_seek_unspecified
 :seek :?N1
 [:?N1 :cg/category [:N]
    :paradigm ....
 ]
 ]
 
[:Category_S1
 :proto/elaborates :Category
 :profile [:?S1]
 ]
 [:?S1 :cg/category [:S] :paradigm ...]
]
[:Category
 :proto/hasParameter :profile
 :proto/hasParameter :seek
 :proto/hasPrameter :syntagm
]

TODO: How do we handle the namepaces for :?X ?


Each category has the following prototype
```
[:category 
 :rdf/type :proto/Prototype
 :proto/hasParameter :profile
 :proto/hasParameter :seeks
 :proto/hasParameter :syntagm
 :proto/hasParameter :paradigm
 ]
```
- profile := [_named-category_, ...], reflecting the sub-elements being referenced in the construction. 
- named category is a unique identifier within the chunk-graph,
  starting with category
- category is one of S, N, D
- seeks := named category
- syntagm := [x ...]
- x is one of #{named-category, _}
  where - matches tokens in a corresponding lexical form.

And
```
  (add-lexical-entry!
   [:enLex/world
    :cg/category [[:N1] -]
    :cg/syntagm [:enForm/world]
    :cg/semantics '(fn[{:keys [N1]}]
                     [[N1 :owl/sameAs :wd/Q16502]]
                     )
    :paradigms {:?N1 (add (make-chunk [:N :rdf/type :Person]))}]) ???
```

A reference has the following elements:
- a profile (one of N S D)
- a numeric index s.t. co-referential elements of a syntagm share the same index
- an optional gloss, e.g. 'world' or 'addressee'
- optional subordinate matching references e.g. (N1:N2 ...) is N1, seeking some N2.


A syntagm consists of a vector of references R followed by a series of matching references and forms.

R consists of a direct reference followed by zero or more indirect references.



(add-lexical-entry!
  `([:N] :enForm/world)
    :cg/semantics '(fn[{:keys [N]}]
                     [[N :owl/sameAs :wd/Q16502]]
                     )]))

(G :hasEntry (mint-kwi :LexicalEntry '([:N] :enForm/world))
(mint-kwi :LexicalEntry '([:N-world] :enForm/world))
  :elaborates :Category_N1_-
  :contents [:enForm/world])
  :bindings {:N :N-world}
  :cg/semantics '(fn[{:keys [N-world]}]
                     [[N-world :owl/sameAs :wd/Q16502]]
                     )]))

(add-lexical-entry!
  `([:S] :enForm/hello :N)
  :cg/semantics '(fn [{:keys [D S N]}]
                       [S :rdf/type :Greeting
                          :greeter (the (D speaker))
                          :addressee N])
  {:?N #{(parse "some person")}})
                          
With derivable category
```
(N -)
```
Which elaborates
(N:* -)

#### Paradigms for matching variables

Each lexical entry must have a _paradigm_, which is a function that
takes arguments corresponding to the discourse _D_, the return varible
, and any other matching variables. It returns a number from 0 to 1
corresponding with its degree of match. These parameters are taken
together because some entries may be more or less suited to a given
discourse, or pairs of arguments might function as an ensemble,
e.g. (S0 N1 eat N2)  might have N1=horse or person and N2 = grass or steak.

```
(lexical-entry 
   {
    :profile [:?N0]
    :syntagm [:enForm/some :?N1]
    :paradigm (fn [D N1] (if (D N1 :rdf/type :rdfs/Class) 1 0))
    :semantics (fn [D N1]
                 (add D [N0 :rdf/type N1]))
    }
    [[:?N0 :category :N]
     [:?N1 :category :N]])
```

### Exemplars

Word definitions are always grounded in specific tokens of a word form
occurring in a discourse, and are intended as examples to be used as a
resource in interpreting novel texts.

The contents of novel texts are matched to examplars in previous texts
as _precedents_ using an analogy mechanism, and bound to the
precedents using the prototypes regime. Each newly processed text can
then serve as the basis for future precedents. The graph of radiating
precedents can be informed by distributional data which should inform
the regime for retrieving felicitous precedents, i.e. precedents prove
to be valuable resources for understanding how a given lexical pattern
is being used in a particular context. This is a semi-supervised
training scheme, where human analysts can review and revise the set of
hand-coded precedents, which then inform a large number of
automatically assigned values.


- IDEA: It should be possible to start with a completely automatically
  extracted set of relations. If we think of the set of precedents as
  a spiral, each unprecedented word form would establish the ancestor
  for all future elaborations. As subsequent exemplars of that token
  appear, a similarity measure would then retreive the 'closest'
  precedent.
  
  From there, we can elaborate the representation of any given
  precendent. Part of the retrieval mechanism involves maximizing the
  most useful chumks, which means optimizing use of information added
  by hand.
  
  
  
## License

Copyright © 2020 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
