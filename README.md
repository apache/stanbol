# Apache Stanbol

Apache Stanbol is a modular set of components and HTTP services for
semantic content management.


## Building Stanbol

To build Stanbol you need a JDK 1.6 and Maven 2.2.1 installed. You probably
need

    $ export MAVEN_OPTS="-Xmx512M -XX:MaxPermSize=128M"

The Kres build is currently disabled by default, as it uses a few artifacts
that are not available in public Maven repositories.

If you want to include Kres in your build, activate the kres Maven profile
(add -P kres to the mvn command line) and install a few artifacts to your 
local Maven repository. Please go to the Stanbol source directory and type 
the following commands (again, this is only needed if you want to build
kres):

    $ mvn install:install-file -Dfile=kres/lib/owlapi-3.0.0.jar \
         -DgroupId=owlapi -DartifactId=owlapi -Dversion=3.0.0 -Dpackaging=jar

    $ mvn install:install-file -Dfile=kres/lib/HermiT.jar \
         -DgroupId=hermit -DartifactId=hermit -Dversion=1.2.4 -Dpackaging=jar

    $ mvn install:install-file -Dfile=kres/lib/owl-link-1.0.2.jar \
         -DgroupId=owl-link -DartifactId=owl-link -Dversion=1.0.2 -Dpackaging=jar

Then again in the Stanbol source directory type

    $ mvn install

If you want to skip the tests, add '-DskipTests' to the Maven command.


## Creating Eclipse Projects

Go to Stanbol source directory and type

    $ mvn eclipse:eclipse

If you want to recreate already existing Eclipse projects, you have to delete
the old ones first by using 'eclipse:clean'.


## Useful links

  - Documentation will be published and mailing lists info at:
    http://incubator.apache.org/stanbol/

  - Please report bugs at:
    https://issues.apache.org/jira/browse/STANBOL

