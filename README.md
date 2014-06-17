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

## Building Stanbol

To build Stanbol you need a JDK 1.6 and Maven 3.0.3+ installed. You probably
need

    $ export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256M"

The following builds the Apache Stanbol plus available Enhancement Engines and a
default set of linked open data for the EntityHub. If you want to have a ready
to use version of Apache Stanbol, this is the way to go.

In the Apache Stanbol source directory type

    $ mvn install

If you want to skip the tests, use :

    $ mvn install -Dmaven.test.skip=true
    

## Launching the Apache Stanbol Server

The recommended lanchers are packaged under the `launchers/` folder. 

### Launching the Apache Stanbol runnable jar

For running the full launcher you need to execute:

    $ java -Xmx1g -jar launchers/full/target/org.apache.stanbol.launchers.full-0.10.0-incubating-SNAPSHOT.jar

Your instance is then available on <http://localhost:8080>. You can change the
default port number by passing a `-p 9090` options to the commandline launcher.

Upon first startup, a folder named `sling/` is created in the current folder.
This folder will hold the files for any database used by Stanbol, deployment
configuration and logs.

If Stanbol is launched with a FactStore a folder named `factstore` is created
in the current folder. This folder holds the FactStore database (Apache Derby).

### Launching the Apache Stanbol runnable war

For running the full war launcher you need to go to the launcher directory:

    $ cd launchers/full-war

and then execute:

    $ mvn clean package tomcat7:run

Your instance is then available on <http://localhost:8080/stanbol>. You can change the
default port number by passing `-Pstanbol.port=9090` property to maven.

### Running Apache Stanbol on a application container

For running the full war launcher on a external application container, just deploy
there in your usual way the file `launchers/full-war/target/stanbol.war`.


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


## Debugging an Apache Stanbol Instance from Eclipse

To debug a locally running Stanbol instance from eclipse, run the stanbol
launcher with::

    $ java -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n \
           -jar org.apache.stanbol.some.launcher-[VERSION].jar -p 8080

In eclipse, you can then create a new "Debug Configuration" with type "Remote
Java Application" and connect it to localhost on port 8787.


## Build without SNAPSHOTs from Apache Repository

Apache Stanbol deployes current SNAPSHOTS of components from the trunk to the
Apache SNAPSHOT Maven repository. This is done on each succesful Jenkins build
at https://builds.apache.org/view/S-Z/view/Stanbol/job/stanbol-trunk/

To locally build Stanbol without loading available SNAPSHOTs from the Apache
SNAPSHOT repository, use can use the 'no-snapshot-dep' profile.

    $ mvn clean install -Pno-snapshot-dep

This profile especially useful if you are preparing a release and want to
ensure that there are no dangling SNAPSHOT dependencies that can not be
resolved from within the locally available components.


## License check via the Apache's Release Audit Tool (RAT)

To check for license headers within the source code Stanbol uses the RAT Maven
plugin [1]. You can activate a 'rat:check' by using the 'rat' Maven profile.

For example to check the licenses in the Stanbol Framework use

    $ mvn install -Prat


## Release Apache Stanbol

You should read [1,2] before doing any release related actions.

To do a release test build, you have to activate the 'apache-release' profile.
For building Apache Stanbol plus signing the artifacts as it would be done during
a release you can use

    $ mvn install -Papache-release

The 'apache-release' profile will be automatically activated when the Maven
release plugin [3] is used. For doing official release you start with

    $ mvn release:prepare

[1] http://www.apache.org/dev/#releases
[2] http://incubator.apache.org/guides/releasemanagement.html
[3] http://maven.apache.org/plugins/maven-release-plugin/


## Useful links

  - Documentation will be published and mailing lists info on [the official
    Stanbol page](http://incubator.apache.org/stanbol/)

  - Please report bugs on the [Apache issue tracker](
    https://issues.apache.org/jira/browse/STANBOL)

