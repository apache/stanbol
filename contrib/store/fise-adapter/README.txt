IKS FISE Persistence Store Adapter
==================================

This bundle implements a storage engine for FISE that stores a ContentItem's metadata in a persistence store and its content in a file.
It implements the eu.iksproject.fise.servicesapi.Store interface.


How to deploy
=============

after 
>mvn clean install
you can upload the jars in target folder to felix

Run
===
see README.txt of eu.iksproject.persistencestore.rest

Remember that all other components that provide the service eu.iksproject.fise.servicesapi.Store should be deactivated.