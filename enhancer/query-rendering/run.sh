#!/bin/sh

CP=bin:resource:`echo resource/am/*.jar | tr " " ":"`:`echo lib/*.jar | tr " " ":"`
JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home/

ant clean && ant && \
echo ${CP} && ${JAVA_HOME}/bin/java -cp ${CP} -Xmx1g org.apache.stanbol.enhancer.interaction.Start 
