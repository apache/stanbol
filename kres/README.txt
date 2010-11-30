How to build KReS and use it with FISE.

1. Install and run KReS in FISE

1.1 build FISE if it is not
1.2 set environment variables for JAVA_HOME (1.6) and MVN_HOME (2.2.1) 
1.3 launch $./heavyInstall -fiseHome < your FISE parent directory > [-a < the host address>] [-p < the port >]


2. Just install KReS

2.1. Set the following environment variables
   - export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=128M"
   - export JAVA_HOME=<Java version 6>

2.2. Build Clerezza

2.3. Build and run  FISE as usually
   - mvn -DskipTests install (in the FISE root directory)
   - cd lauchers/sling/target
   - rm -rf slim (in case of previous sling installations)
   - java -jar -Xmx512m eu.iksproject.fise.launchers.sling-0.9-SNAPSHOT.jar

2.4. Build and deploy KReS
   - go in the main KReS dir (the directory where this README is located)
   - ./install
   - go with your browser to http//localhost:8080/kres


KReS JavaDocs are available at http://stlab.istc.cnr.it/documents/iks/kres/