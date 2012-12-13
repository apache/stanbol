usermanager
===========

A usermanager for stanbol. It provides a felix webconsole plugin as well as the following HTTP resources to manage users and roles, the HTTP services are described in terms of curl-commands and assume Stanbol to be running on localhost.

Note that users are uniquely identified by their clerezza:userName (= login) but may also have a foaf:name (= full name).

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

Change user details. Multiple change blocks may appear in a message. If old value isn't specified, the corresponding triple won't be removed from the system.

e.g. change user name:

    curl -i -X POST -H "Content-Type: text/turtle" --user admin:admin \
        --data " @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \
                 @prefix cz: <http://clerezza.org/2009/08/platform#> . \
                 @prefix : <http://stanbol.apache.org/ontologies/usermanagement#>. \
                 [] a :Change;  \
                    cz:userName 'hugob'; \
                    :predicate cz:userName; \
                    :oldValue 'hugob'; \
                    :newValue 'tristant' . " \
          http://localhost:8080/user-management/change-user

e.g. add email:

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

Get user Turtle :

    curl --user admin:admin -H "Accept:text/turtle" http://localhost:8080/user-management/user/anonymous

Get user roles :

   curl --user admin:admin -H "Accept:text/turtle" http://localhost:8080/user-management/roles/anonymous

