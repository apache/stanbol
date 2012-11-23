usermanager
===========

a usermanager for stanbol. It provides a felix webconsole plugin as well as the following HTTP resources to manage users and roles, the HTTP services are described in terms of curl-commands and assume Stanbol to be running on localhost

Add user:

    curl -i -X POST -H "Content-Type: text/turtle" \
         --user admin:admin \
         --data \
         ' @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . 
         @prefix foaf: <http://xmlns.com/foaf/0.1/> . 
         @prefix cz: <http://clerezza.org/2009/08/platform#> . 

          [] a foaf:Agent ; 
             cz:userName "Hugo Ball" . ' \
         http://127.0.0.1:8080/user-management/add-user

Delete user:

    curl -i -X POST -H "Content-Type: text/turtle" \
         --user admin:admin \
         --data \
         ' @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . 
         @prefix foaf: <http://xmlns.com/foaf/0.1/> . 
         @prefix cz: <http://clerezza.org/2009/08/platform#> . 

          [] a foaf:Agent ; 
             cz:userName "Tristan Tzara" . ' \
         http://127.0.0.1:8080/user-management/delete-user

Change userName :

    curl -i -X POST -H "Content-Type: text/turtle" --user admin:admin \
        --data " @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \
                 @prefix cz: <http://clerezza.org/2009/08/platform#> . \
                 @prefix : <http://stanbol.apache.org/ontologies/usermanagement#>. \
                 [] a :Change;  \
                    :predicate cz:userName; \
                    :oldValue 'Hugo Ball'; \
                    :newValue 'Tristan Tzara' . " \
          http://127.0.0.1:8080/user-management/change-user

Plus get user Turtle :

    curl --user admin:admin -H "Accept:text/turtle"
    http://localhost:8080/user-management/user/anonymous
