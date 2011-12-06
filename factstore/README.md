<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Apache Stanbol FactStore

The FactStore is a component that let's use store relations between entities
identified by their URIs. A relation between two or more entities is called a
*fact*. The FactStore let's you store N-ary facts. In consequence you can
store relations between N participating entities.

## Documentation

To get the latest documentation you should start your copy of Apache Stanbol
and navigate your browser to http://localhost:8080/factstore. There you will
find more information and the documentation of the FactStore's REST API.

## Example

Imagine you want to store the fact that the person named John Doe works for
the company Winzigweich. John Doe is represented by the URI
http://www.doe.com/john and the company by http://www.winzigweich.de. This
fact is stored as a relation between the entity http://www.doe.com/john and
http://www.winzigweich.de.

For this, we first need to create a so called fact schema that tells the
FactStore what we would like to store. A fact schema has a unique name (often
an URI is used) to identify it. To specify what kinds of entities we would
like to store, we specify the type of the entities. Each type has an URI and
should be defined by some ontology. For example, we can use the ontology
specified by http://schema.org/.

According to http://schema.org/ a person is of type http://schema.org/Person
and an organization is of type http://schema.org/Organization. We will use
these type information to specify the fact schema
http://factschema.org/worksFor. The specification of a fact schema is written
in JSON-LD, like this:

    {
      "@context" : {
        "#types"  : {
          "person"       : "http://schema.org/Person",
          "organization" : "http://schema.org/Organization"
        }
      }
    }

To create this fact schema in the FactStore we have to store it in a *.json
file, e.g. worksFor.json, and PUT it into the FactStore. The path to put the
fact schema is `/factstore/facts/{factSchemaName}`. So for our example this
would be `/factstore/facts/http://factschema.org/worksFor`. Unfortunately,
this is not a valid URI so that we have to URL-encode the name of the fact
schema. This leads to
`/factstore/facts/http%3A%2F%2Ffactschema.org%2FworksFor`.

_Note_: If you want to avoid this URL-encoding step, you should chose another
name for your fact schema that is not an URI by itself. You are free to do so!

Now to PUT the `worksFor` fact schema we can use this cURL command.

    curl http://localhost:8080/factstore/facts/http%3A%2F%2Ffactschema.org%2FworksFor -T worksFor.json

After creating the fact schema we can store the fact that John Doe works for
Winzigweich by POSTing it to the FactStore. The fact is specified in JSON-LD
syntax. The `@profile` defines the fact schema where this fact belongs to.

    {
      "@profile"     : "http://factschema.org/worksFor",
      "person"       : { "@iri" : "http://www.doe.com/john" },
      "organization" : { "@iri" : "http://www.winzigweich.de"}
    }

Now we can POST this fact, e.g. stored in fact.json, to the FactStore at
`/factstore/facts`. By using cURL it would be this command:

    curl -d @fact.json -H "Content-Type: application/json" http://localhost:8080/factstore/facts

On success this will return a 201 (Created) and the URI of the newly created
fact in the location header of the response. To retrieve a fact you can GET it
from the returned URI.
