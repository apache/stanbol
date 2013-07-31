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

User Management
===============

A user manager for Stanbol. It provides a Felix Web Console plugin as well as various HTTP-accessible resources to manage users, roles and permissions. 
Data is persisted in the system graph, and access control is managed by existing structures in that graph.
Server-side the data is accessed through JAX-RS methods (JSR 311, http://jcp.org/en/jsr/detail?id=311).

## Tests
Functional tests are available under stanbol/integration-tests (package org.apache.stanbol.commons.usermanagement.it). See that documentation for further details, but in short, with a running system:
   cd stanbol/integration-tests
   mvn -o test -Dtest.server.url=http://localhost:8080 -Dtest=UserManagement*Test

## Access Modes
Three different kinds of access are available:

### HTML 
Primarily used by the Felix Web Console plugin running in a browser. Server-side the JAX-RS methods typically delegate to RdfViewable objects which provide HTML serializations, created from combinations of resources in the graph and FreeMarker templates (augmented with RDF view components).

Client-side, regular HTML + Javascript is used, helped by jQuery (mostly Ajax methods) and jQueryAPI (mostly dialogues).

### API
Endpoint-style access is provided to modify data using custom Turtle format messages.

### RESTful 
Direct access is provided to modify data associated with named resources (URIs).

## User Model
@@TODO

Note that users are uniquely identified by their cz:userName (= login) but may also have a foaf:name (= full name).

## URI Schemes
The following assumes your Stanbol instance is running on localhost port 8080.

### Primary Resources
@@TODO media types

http://localhost:8080/user-management/users/{username}
http://localhost:8080/user-management/roles/{username}

### API Endpoints
http://localhost:8080/user-management/add-user
http://localhost:8080/user-management/delete-user

### HTML Helper Resources

curl --user admin:admin http://localhost:8080/user-management/users/anonymous/permissionsCheckboxes

## API Examples
The following HTTP services are 
described in terms of curl-commands and assume Stanbol to be running on localhost.

The following assumes your Stanbol instance is running on localhost port 8080.

Add user:

    curl -i -X POST -H "Content-Type: text/turtle" \
        --user admin:admin \
        --data \
         ' @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . 
         @prefix foaf: <http://xmlns.com/foaf/0.1/> . 
         @prefix cz: <http://clerezza.org/2009/08/platform#> . 
          [] a foaf:Agent ; 
             cz:userName "hugob" . ' \
         http://localhost:8080/user-management/add-user

Delete user:

    curl -i -X POST -H "Content-Type: text/turtle" \
         --user admin:admin \
         --data \
         ' @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . 
         @prefix foaf: <http://xmlns.com/foaf/0.1/> . 
         @prefix cz: <http://clerezza.org/2009/08/platform#> . 

          [] a foaf:Agent ; 
             cz:userName "tristant" . ' \
         http://localhost:8080/user-management/delete-user

[TODO: also add password, maybe showing 2 options one setting encryed password 
(as its stored) and other transmitting clear text password]

Change user details. Multiple change blocks may appear in a message. If old 
value isn't specified, the corresponding triple won't be removed from the system.

e.g. change user name:

    curl -i -v -X POST -H "Content-Type: text/turtle" --user admin:admin \
         --data " @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \
                  @prefix cz: <http://clerezza.org/2009/08/platform#> . \
                  @prefix : <http://stanbol.apache.org/ontologies/usermanagement#>. \
                    [] a :Change;  \
                       cz:userName 'hugob'; \
                       :predicate cz:userName; \
                       :oldValue 'hugob'; \
                       :newValue 'tristant' . " \
         http://localhost:8080/user-management/change-user

e.g. add email (replacing a previous address if any):

    curl -i -X POST -H "Content-Type: text/turtle" --user admin:admin \
        --data " @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \
                 @prefix foaf: <http://xmlns.com/foaf/0.1/> . \
                 @prefix cz: <http://clerezza.org/2009/08/platform#> . \
                 @prefix : <http://stanbol.apache.org/ontologies/usermanagement#>. \
                 [] a :Change;  \
                    cz:userName 'hugob'; \
                    :predicate foaf:mbox; \
                    :newValue <mailto:hugob@example.org> . " \
          http://localhost:8080/user-management/change-user

## REST Access Examples

Get user Turtle :

    curl --user admin:admin -H "Accept:text/turtle" http://localhost:8080/user-management/users/anonymous

Note: Other formats are supported, e.g. you may use -H "Accept: application/rdf+xml"

Get user roles :

   curl --user admin:admin -H "Accept:text/turtle" http://localhost:8080/user-management/roles/anonymous

