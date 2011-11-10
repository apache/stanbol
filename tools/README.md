# Utilities for sysadmin and developers

## stanbol

A startup script to put in your /etc/init.d/ on Linux. To install on the
default runlevels on Debian / Ubuntu, run the following command (as root):

    $ update-rc.d stanbol defaults

Don't forget to update the parameters (e.g. path to the launcher jar)
in the beginning of the script.
