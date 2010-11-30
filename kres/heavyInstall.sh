#!/bin/bash

echo "***********************************"
echo "*  IKS KReS version 0.6-SNAPSHOT  *"
echo "***********************************"



host=localhost
port=8080
fise_home=/

fecth_host=0
fecth_port=0
fecth_fise_home="no"

found_fise_home=0

test="ok"

counter=0
for i in $*; do

	
	if [ "$fetch_host" = "1" ] ; then
		host="$i"
		let fetch_host=0
	else 
		if [ "$fetch_port" = "1" ] ; then
			port="$i"
			let fetch_port=0
		else 
			if [ "$fecth_fise_home" = "1" ] ; then
				fise_home="$i"
				let fecth_fise_home=0
				let found_fise_home=1
			else
				case $i in
				"-a")
					let fetch_host=1
					;;
				"-p")
					let fetch_port=1
					;;
				"-h")
					echo "install usage:"
					echo "	-h Help."
					echo "  -fiseHome Sets the home of your local FISE installation."
					echo "	-a Set the address of the host."
					echo "	-p Set the port of the host."
					exit 0
					;;
				"-fiseHome")
					let fecth_fise_home=1
					;;
				esac
			fi
		fi
	fi
	
	
	let counter+=1
	
done


if [ "$found_fise_home" = "1" ] ; then

	echo HOST $host
	echo PORT $port
	echo FISE_HOME $fise_home

	cd ext
	cp list.xml $fise_home/launchers/sling/src/main/bundles/
	echo Copied list.xml to $fise_home/launchers/sling/src/main/bundles/
	
	cp pomPS.xml $fise_home/stores/persistencestore/persistencestore/pom.xml
	echo Copied pom.xml to $fise_home/stores/persistencestore/persistencestore/pom.xml
	
	cp pomPS-jena.xml $fise_home/stores/persistencestore/persistencestore-jena/pom.xml
	echo Copied pom.xml to $fise_home/stores/persistencestore/persistencestore-jena/pom.xml
	
	cp pomPS-tdb.xml $fise_home/stores/persistencestore/persistencestore-tdb/pom.xml
	echo Copied pom.xml to $fise_home/stores/persistencestore/persistencestore-tdb/pom.xml
	
	cp pomPS-adapter.xml $fise_home/stores/persistencestore/persistencestore-adapter/pom.xml
	echo Copied pom.xml to $fise_home/stores/persistencestore/persistencestore-adapter/pom.xml
	
	cd ..
	
	
	sling_url=http://$host:$port/system/console
	
	pushd $fise_home/launchers/sling/
	
	$MVN_HOME/bin/mvn -DskipTests install
	echo Installed sling.
	
	cd target
	
	rm -rf sling
	echo Starting sling on host $host and port $port.
	
	$JAVA_HOME/bin/java -jar -Xmx512m eu.iksproject.fise.launchers.sling-0.9-SNAPSHOT.jar -a $host -p $port &
	
	popd
	
	cd lib
	$MVN_HOME/bin/mvn install:install-file -Dfile=owlapi-3.0.0.jar -DgroupId=owlapi -DartifactId=owlapi -Dversion=3.0.0 -Dpackaging=jar
	$MVN_HOME/bin/mvn install:install-file -Dfile=HermiT.jar -DgroupId=hermit -DartifactId=hermit -Dversion=1.2.4 -Dpackaging=jar
	$MVN_HOME/bin/mvn install:install-file -Dfile=owl-link-1.0.2.jar -DgroupId=owl-link -DartifactId=owl-link -Dversion=1.0.2 -Dpackaging=jar
	cd ..
	$MVN_HOME/bin/mvn install -DskipTests -PinstallBundle -Dsling.url=$sling_url
	
	pushd $fise_home
	
	cd stores/persistencestore/persistencestore
	$MVN_HOME/bin/mvn install -DskipTests -PinstallBundle -Dsling.url=$sling_url
	echo Installed persistencestore.
	
	cd ../persistencestore-jena/
	$MVN_HOME/bin/mvn install -DskipTests -PinstallBundle -Dsling.url=$sling_url
	echo Installed persistencestore-jena.
	
	cd ../persistencestore-tdb/
	$MVN_HOME/bin/mvn install -DskipTests -PinstallBundle -Dsling.url=$sling_url
	echo Installed persistencestore-tdb.
	
	cd ../persistencestore-adapter/
	$MVN_HOME/bin/mvn install -DskipTests -PinstallBundle -Dsling.url=$sling_url
	echo Installed persistencestore-adapter.
	
	popd
else
	echo "FISE HOME not specified"
	echo "type ./heavyintall.sh -h" for usage
fi