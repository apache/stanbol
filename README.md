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

If you want to skip the tests, add `-DskipTests` to the Maven command.


## Launching the Stanbol server

The recommended lanchers are packaged under the `launchers/` folder. For
instance:

    $ java -Xmx1g -jar launchers/full/target/org.apache.stanbol.launchers.full-0.9-SNAPSHOT.jar

Your instance is then available on <http://localhost:8080>. You can change the
default port number by passing a `-p 9090` options to the commandline launcher.

Upon first startup, a folder named `sling/` is created in the current folder.
This folder will hold the files for any database used by Stanbol, deployment
configuration and logs.


## Preloading the Entity Hub cache with a DBpedia index

TODO: write me!


## Importing the source code as Eclipse projects

Eclipse is the most popular IDE among Stanbol developers. Here are
instructions to get you started with this IDE. For other IDEs / editors,
please refer to their documentation and maven integration plugins.

To generate the Eclipse project definition files, go to Stanbol source
directory and type:

    $ mvn eclipse:eclipse

If you want to recreate already existing Eclipse projects, you have to delete
the old ones first by using `eclipse:clean`.

Then in Eclipse, right click on the `Project Explorer` panel and select
your source folder from the following menu / import wizard:

    > Import... > General > Import Existing Projects into Workspace

You will also need to setup the build path variable `M2_REPO` pointing to
`~/.m2/repository` (where `~` stands for the path to your home folder). To set
up this variable go to:

    > Window > Preferences > Java > Build Path > Classpath Variables > New...

If you plan to contribute patches to the project, please ensure that your style
follow the official sun java guidelines with 4 space indents (no tabs). To
ensure that your files follow the guidelines you can import the formatter
definitions avaiable in the `conventions/` folder:

    > Window > Preferences > Java > Code Style > Formatter > Import...

You can then apply the formatter to a selected area of a Java source code files
by pressing `Shift+Ctrl+F`.


## Debugging a Stanbol instance from Eclipse

To debug a locally running Stanbol instance from eclipse, run the stanbol
launcher with::

    $ java -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n \
           -jar org.apache.stanbol.some.launcher.0.9-SNAPSHOT.jar -p 8080

In eclipse, you can then create a new "Debug Configuration" with type "Remote
Java Application" and connect it to localhost on port 8787.


## Useful links

  - Documentation will be published and mailing lists info on [the official
    Stanbol page](http://incubator.apache.org/stanbol/)

  - Please report bugs on the [Apache issue tracker](
    https://issues.apache.org/jira/browse/STANBOL)

