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


## Useful links

  - Documentation will be published and mailing lists info at:
    http://incubator.apache.org/stanbol/

  - Please report bugs at:
    https://issues.apache.org/jira/browse/STANBOL

