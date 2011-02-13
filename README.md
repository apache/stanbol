# Apache Stanbol

Apache Stanbol is a modular set of components and HTTP services for
semantic content management.


## Building Stanbol

To build Stanbol you need a JDK 1.6 and Maven 2.2.1 installed. You probably
need

    $ export MAVEN_OPTS="-Xmx512M -XX:MaxPermSize=128M"

The Kres build is currently disabled by default.

If you want to include Kres in your build, activate the kres Maven profile
(add -P kres to the mvn command line). 

Then in the Stanbol source directory type

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

