VIE based Stanbol Enhancer UI
=============================

Adapter to add a VIE based interface for the Stanbol Enhancer

Deploy
------

After building stanbol in a different folder you can build and deploy this bundle by using the following command:

    mvn install -DskipTests -PinstallBundle -Dsling.url=http://localhost:8080/system/console

Development Guide
=================

The widget development is done in CoffeeScript. Use the following command to 
automatically compile the script in the background
    
    coffee -c -w -o lib annotate.coffee
    
This will as well show you compile errors right after each save.

To simplify work with the deep directory structure I created a directory for
shortcuts to the most important directories called 'sc':

    ls -ln sc/
    lrwxrwxrwx 1 1000 1000  52 2011-06-01 11:44 java -> ../src/main/java/org/apache/stanbol/enhancer/webvie/
    lrwxrwxrwx 1 1000 1000  63 2011-06-01 11:43 static -> ../src/main/resources/org/apache/stanbol/enhancer/webvie/static
    lrwxrwxrwx 1 1000 1000 135 2011-06-01 11:44 template -> ../src/main/resources/org/apache/stanbol/enhancer/webvie/templates/org/apache/stanbol/enhancer/webvie/resource/EnhancerVieRootResource/

Annotate widget API
===================
Options:
--------

Methods:
--------
**enable**:
    Analyze text and highlight TextAnnotations. Make TextAnnotations 
    interactive.

**disable**:
    Remove TextAnnotation highlighting.

Events
------
**select**:
    Triggered on annotating a suggested TextEnhancement

**decline**:
    Triggered on declining a suggested TextEnhancement

**remove**:
    Triggered when an annotation is removed
    
    function (event, ui)
    
    The ui object has the following attributes:
        `textEnhancement`,
        `entityEnhancement`
        `linkedEntity`

