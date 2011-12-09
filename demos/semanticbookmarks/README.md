# Apache Stanbol Semantic Booksmarks Demo

This is a Mozilla Firefox extension that supports semantic bookmarks. In
this version you can 'like' or 'dislike' web pages.

* Supported Firefox versions    : 3.0 - 7.x
* Tested on Ubuntu with Firefox : 3.6.22

## Packaging

The packaging is done via Apache Ant script 'build.xml'. Execute via:

  $ ant package
  
The resulting *.xpi file will be located in the 'target' folder. To clean the
target folder use

  $ ant clean
  
To clean a previous packaged version and package a new one in one step use

  $ ant clean package
  
## Installing

Open your Firefox browser and install a new extension. Choose the created
*.xpi file and proceed with the installation. Normally, a Firefox
restart is required after install.

You can also just use File->Open in Firefox and select the *.xpi file.

## Update

Just re-install the plugin. No prior de-installation required.

## Usage

You will see a small Stanbol icon in the status bar after installing the
plugin. Click on the icon to see its context menu.

