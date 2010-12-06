                      Welcome to Apache Stanbol
                     ---------------------------

Building Stanbol:
-----------------

To build Stanbol you need a JDK 1.6 and Maven 2.2.1 installed. You probably
need

  $ export MAVEN_OPTS="-Xmx512M -XX:MaxPermSize=128M"

Before you can build Stanbol you need to install a few artifacts to your local
Maven repository. This step is needed at the moment because these artifacts are
not available via any public Maven repository. Please go to the Stanbol source
directory and type the following commands:

  $ mvn install:install-file -Dfile=kres/lib/owlapi-3.0.0.jar \
       -DgroupId=owlapi -DartifactId=owlapi -Dversion=3.0.0 -Dpackaging=jar

  $ mvn install:install-file -Dfile=kres/lib/HermiT.jar \
       -DgroupId=hermit -DartifactId=hermit -Dversion=1.2.4 -Dpackaging=jar

  $ mvn install:install-file -Dfile=kres/lib/owl-link-1.0.2.jar \
       -DgroupId=owl-link -DartifactId=owl-link -Dversion=1.0.2 -Dpackaging=jar

Then again in the Stanbol source directory type

  $ mvn install

If you want to skip the tests, add '-DskipTests' to the Maven command.


Creating Eclipse Projects:
--------------------------

Go to Stanbol source directory and type

  $ mvn eclipse:eclipse

If you want to recreate already existing Eclipse projects, you have to delete
the old ones first by using 'eclipse:clean'.


Issue Tracking:
---------------

Please report bugs at
  
  https://issues.apache.org/jira/browse/STANBOL


