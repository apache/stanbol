# Apache Stanbol Semantic Booksmarks Demo v0.1

This is a Mozilla Firefox extension that supports semantic bookmarks. In
this early version you can 'like' or 'dislike' web pages.

* Supported Firefox versions    : 3.0 - 8.x
* Tested on Ubuntu with Firefox : 3.6.22, 8.0

## Installing

Open your Firefox browser and install a new extension. Choose the created
or downloaded *.xpi file and proceed with the installation. Normally, a
Firefox restart is required after install.

You can also just use File->Open in Firefox and select the *.xpi file.

## Update

Just re-install the plugin. No prior de-installation required.

## Usage

Make sure you have a running Stanbol server with activated FactStore and
EntityHub.

You will see a small Stanbol icon in the status bar (aka addon bar) after
installing the plugin in your Mozilla Firefox browser. If you don't see
this icon make sure the status bar is visible (newer versions of Firefox
hide it by default). Click on the icon to see its context menu.

First you need to setup your running Stanbol instance via the 'Settings'
menu. You have to configure the Stanbol base URI which is normally
'http://localhost:8080'. Then you click the 'Initialize Stanbol' button.
Next step is to create your user entity by filling name and an URI identifying
your person. Then click 'Create User'. Now everything is set up.
(See Known Bugs!)

After the setup you can 'like' and 'dislike' pages by clicking on the menu
entries. The page that is currently visible in the main window will be
liked/disliked. To see all your liked and disliked pages select 'Show Pages'
from the menu.

## Known Bugs

- When using the settings dialog and changing the Stanbol base URI you have
  to click OK after changing the URI. Otherwise this change will have no effect.

- Some links on the overview page with all liked and disliked pages will not
  work and result in an internal server error. This is because of a missing
  feature of Stanbol's EntityHub.

## Build from Source

The packaging is done via Apache Ant script 'build.xml'. Execute via:

  $ ant package
  
The resulting *.xpi file will be located in the 'target' folder. To clean the
target folder use

  $ ant clean
  
To clean a previous packaged version and package a new one in one step use

  $ ant clean package
