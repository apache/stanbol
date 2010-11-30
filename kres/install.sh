#!/bin/bash

echo "***********************************"
echo "*  IKS KReS version 0.6-ALPHA  *"
echo "***********************************"

host=localhost
port=8080

fecth_host=0
fecth_port=0


counter=0
for i in $*; do

	echo parameter $i
	
	if [ "$fetch_host" = "1" ] ; then
		host=$i
		let fetch_host=0
		echo HOST is $host
	else 
		if [ "$fetch_port" = "1" ] ; then
			port=$i
			let fetch_port=0
			echo PORT is $port
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
				echo "	-a Set the address of the host."
				echo "	-p Set the port of the host."
				exit 0
				;;
			esac
		fi
	fi
	
	
	let counter+=1
	
done

sling_url=http://$host:$port/system/console

echo SLING URL "$sling_url"



cd lib
mvn install:install-file -Dfile=owlapi-3.0.0.jar -DgroupId=owlapi -DartifactId=owlapi -Dversion=3.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=HermiT.jar -DgroupId=hermit -DartifactId=hermit -Dversion=1.2.4 -Dpackaging=jar
mvn install:install-file -Dfile=owl-link-1.0.2.jar -DgroupId=owl-link -DartifactId=owl-link -Dversion=1.0.2 -Dpackaging=jar
cd ..
mvn install -DskipTests -PinstallBundle -Dsling.url=$sling_url

