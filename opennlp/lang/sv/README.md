# Data files Bundles for OpenNLP

This source repository only holds the pom.xml file and folder structure of this bundle.

To avoid loading subversion repository with large binary files this artifact has to be build and deployed manually to retrieve precomputed models from other sites.


## Downloading the OpenNLP statistical model 

The OpenNLP models are downloaded from 

    http://opennlp.sourceforge.net/models-1.5

This url is defined as property in the 'pom.xml'
The list of downloaded file is defined within the 'download_models.xml'

## NOTE

Using this bundles is only an alternative of manually copying the required OpenNLP models to the '{stanbol-installation}/sling/datafiles'. However note that OpenNLP uses 'se' as prefix for Swedish however 
the official ISO language code is 'sv'! Therefoer the original model files need 
to be renamed from

    se-**
    
to

    sv-**
    
The build process of this bundle does this by default. However when copying
the model files to the '{stanbol-installation}/sling/datafiles' this MUST BE done
manually!
