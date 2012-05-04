# Apache Stanbol Semantic Annotation Plugin

aka Apache Stanbol SAP

This is a Mozilla Firefox extension that let's you analyse any web page by
sending its plain text content to a running instance of Stanbol Enhancement
engines. The raw annotation results will be displayed in an extra window.

* Supported Firefox versions    : 3.0 - 6.x
* Tested on Ubuntu with Firefox : 3.6.22

## Packaging the SAP

The packaging is done via Apache Ant script 'build.xml'. Execute via:

  $ ant package
  
The resulting *.xpi file will be located in the 'target' folder. To clean the
target folder use

  $ ant clean
  
To clean a previous packaged version and package a new one in one step use

  $ ant clean package
  
## Installing

Open your Firefox browser and install a new extension. Choose the created
'assap.xpi' file and proceed with the installation. Normally, a Firefox
restart is required after install.

You can also just use File->Open in Firefox and select the *.xpi file.

## Update

Just re-install the plugin. No prior de-installation required.

## Usage

You will see a small Stanbol icon in the status bar after installing the
plugin. Click on the icon to see its context menu.

 - 'Run Analysis' - sends the contents of the current web page to the Stanbol
   server configured via 'Settings'. A window will open up and display the
   raw annotation results. If the window is already open the results will be
   updated.
 - 'Settings' - let's you configure the Stanbol server URL. Additionally, you
   can activate automatic analyzation of any web page once it is loaded in the
   browser.
 - 'About' - displays a dialog with information about the plugin.
