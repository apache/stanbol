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

# Apache Stanbol

Apache Stanbol is a modular set of components and HTTP services for
semantic content management.


## Prepare Your Environment

To build Apache Stanbol you need a JDK 1.6 and Apache Maven 2.2.1
installed. You probably need to set these Maven options to increase
the available amount of memory for the Maven instance.

    $ export MAVEN_OPTS="-Xmx512M -XX:MaxPermSize=128M"

### Third Party Dependencies

Apache Stanbol has a lot of dependencies to third party libraries. Normally,
these are downloaded automatically by Maven during the build process from
the Maven central repository. But Apache Stanbol 0.9.0-incubating has one
depndency that is not available via Maven central. Therefore, you have to
install this manually into your local Maven repository.

Third party dependencies which are not available via Maven central:

  - OWL API version 3.2.3 from http://owlapi.sourceforge.net/
    License: Apache License, Version 2.0

You can download the required dependency for your version of Apache
Stanbol by downloading the Apache Stanbol -deps package from:

http://www.apache.org/dist/incubator/stanbol/apache-stanbol-0.9.0-incubating-deps/

*Note*, this -deps package download is only provided for your convinience.
It is not part of any official Apache Stanbol source release. You can
also generate the -deps package by going to the /deps folder in the source
tree and following the instructions there.

Another alternative would be to download the dependency from their project
website and install it manually in your local Maven repository. You could use
the following Maven command for this:

    $ mvn install:install-file -Dfile=owlapi-bin.jar -Dsource=owlapi-src.jar \
        -DgroupId=net.sourceforge.owlapi -DartifactId=owlapi -Dversion=3.2.3

If you have decided to download the -deps package, you should extract its
content and execute the included script.

    $ install.[sh|bat]

This will also install the deps into your local Maven repository.

### The Build System

The Apache Stanbol build system consists of the following profiles:

   - 'stack'     - DEFAULT - to build the Stanbol Stack
   - 'framework' - to build the Stanbol Framework only

If you build Apache Stanbol from a source tree, the 'stack' profile
is activated by default. If you whish to activate another profile use the
Maven -P command line switch.

If you want to skip the tests, add `-DskipTests` to the Maven command.


## Building the Apache Stanbol Stack

This builds Apache Stanbol including all available Enhancement Engines and a
default set of linked open data for the EntityHub. If you want to have a ready
to use version of Apache Stanbol, this is the way to go.

In the Apache Stanbol source directory type

    $ mvn install

or if you want to clean a previous built version before use

    $ mvn clean install

## Launching the Apache Stanbol Server

The Apache Stanbol server lanchers are packaged under the `launchers/` folder. For
instance:

    $ java -Xmx1g -jar launchers/full/target/org.apache.stanbol.launchers.full-0.9.0-incubating.jar

Your Apache Stanbol server instance is then available on <http://localhost:8080>.
You can change the default port number by passing a `-p 9090` options to the
commandline launcher.

Upon first startup, a folder named `sling/` is created in the current folder.
This folder will hold the files for any database used by Stanbol, deployment
configuration and logs.

If Apache Stanbol is launched with a FactStore a folder named `factstore` is created
in the current folder. This folder holds the FactStore database (Apache Derby).

You can now start to explore Apache Stanbol and its services. Have fun!

## Importing the source code as Eclipse projects

Eclipse is the most popular IDE among Apache Stanbol developers. Here are
instructions to get you started with this IDE. For other IDEs / editors,
please refer to their documentation and maven integration plugins.

To generate the Eclipse project definition files, go to the Apache Stanbol source
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


## Debugging an Apache Stanbol instance from Eclipse

To debug a locally running Apache Stanbol instance from eclipse, run the Apache
Stanbol launcher with::

    $ java -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n \
           -jar org.apache.stanbol.some.launcher.0.9-SNAPSHOT.jar -p 8080

In eclipse, you can then create a new "Debug Configuration" with type "Remote
Java Application" and connect it to localhost on port 8787.


## License check via the Apache's Release Audit Tool (RAT)

To check for license headers within the source code, Apache Stanbol uses the RAT Maven
plugin [1]. You can activate a 'rat:check' by using the 'rat' Maven profile.

For example to check the license headers in Apache Stanbol use

    $ mvn install -Prat


## Apache Stanbol Release Process

You should read [1,2] before doing any release related actions.

To do a release test build, you have to activate the 'apache-release' profile.
For building Apache Stanbol plus signing the artifacts as it would be done during
a release you can use

    $ mvn install -Pstack,apache-release

The 'apache-release' profile will be automatically activated when the Maven
release plugin [3] is used. For doing official release you start with

    $ mvn release:prepare

[1] http://www.apache.org/dev/#releases
[2] http://incubator.apache.org/guides/releasemanagement.html
[3] http://maven.apache.org/plugins/maven-release-plugin/


## Useful links

  - Documentation is published and mailing lists info on [the official
    Stanbol page](http://incubator.apache.org/stanbol/)

  - Please report bugs on the [Apache issue tracker](
    https://issues.apache.org/jira/browse/STANBOL)
