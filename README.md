# parser

This project aims to implement a construction-based NL parser in clojure.

## Installation

Download from http://example.com/FIXME.

## Description

### Parsing
Invoke the parser thus:

```
> (parse <kr> <document>) -> <kr'>

```
Where
- KR := {<chunk-id> {<asGraph> #{<g>},
                     <p> #{<o>...}
                     ...}}
- <p> reflects things like base-activation of the chunk.
- <g> := a graph representng an activation pattern 

The _KR_ will be changed as follows:

- A _chunk_ representing the discourse associated with _document_ will be added to _KR_.
- The document discourse chunk will be a graph with the following characteristics:
  - Each entity spoken of in the document will be a subject, encoded as a KWI with of category 'N'. This in turn will have a chunk in the KR.
  - Each relationship spoken of in the document will be a subject encoded as KWI with category 'S'. This in turn will have a chunk in the KR.
  - Both Ns and Ss will have types, which in turn will be represented as chunks in the KR
  - Each sub-discourse (heading, paragraph, table, etc) in the discourse will be represented as a subject, linked to a chunk.
  

### The KR
The Knowledge Resource or kr is a 'metagraph' which holds your linguistic knowledge, holding a number of specialized graphs with metadata about each graph. 

### Chunks
Many of these graphs are 'chunks', which are usually fairly small
graphs dedicated to representing an 'activation pattern'. Examples of
chunks would be lexical entries, parses and sub-parses, as well as
whole discourses and representations of real-world entities. Each
chunk can be assigned a level of activation reflecting ist
availability to the model at any point in the parsing process.

Note that each chunk has an external aspect reflected in the KR, and
an internal aspect held in the chunk-graph itself.

#### Activation
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

### Categories

This parser is informed by Categorial Grammar to some degree, and each parse and sub-parse is assigned a category constructed as follows:

#### Primitive categories
- :D a unit of discourse
  - Each sentence takes place within a tree of D's, whose root D is
    the top-level discourse
  - Properties whose domain is a discourse would include _partOfDiscourse_, _speaker_, _audience_,  _venue_, _time_,  _hasParticipant_, _hasEvent_, _hasState_, ...
  - Note that we can also model the speaker and listener, as well as
    the occasion, such as a political rally, etc.
  - Discourses can be queried for their contents in the course of parsing, 
    - queries may traverse the tree of discourses and sub-discourses.
- :N a nominal entity
  - Typical of noun phrases
  - Properties would reflect the entities being described
- :S a sentential relationship
  - Typical of verb phrases and whole sentences
  - Each instance of an S will be associated with a type, which will
    typically be associated with a set of roles.
  - Each role will typically be a sub-property of typical thematic
    roles like _agent_, _patient_, etc.


#### Catagory composition

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
```
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
    :cg/catLabel "N"
    :cg/category [:N]
    :cg/semantics '(fn[{:keys [N]}]
                     [[N :owl/sameAs :wd/Q16502]]
                     )]))

```

Actually, I don't like this.

How about:

```
  (add-lexical-entry!
   [:enLex/hello
    :cg/category [:S]
    :cg/pattern  [:enForm/hello :?N]
    :cg/semantics '(fn [{:keys [S N D]}]
                     [[S
                       :sem/eventType :Greeting
                       :sem/addressee N]]
                     )])

```

And
```
  (add-lexical-entry!
   [:enLex/world
    :cg/category [:N]
    :cg/pattern [:enForm/world]
    :cg/semantics '(fn[{:keys [N]}]
                     [[N :owl/sameAs :wd/Q16502]]
                     )]))

```

### Exemplars


## License

Copyright © 2020 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
