<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Utilities for sysadmin and developers

## stanbol

A startup script to put in your /etc/init.d/ on Linux. To install on the
default runlevels on Debian / Ubuntu, run the following command (as root):

    $ update-rc.d stanbol defaults

Don't forget to update the parameters (e.g. path to the launcher jar)
in the beginning of the script.
