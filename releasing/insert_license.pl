#!/usr/bin/perl -w

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#=========================================================
#
# For files that do not yet have an Apache License, insert the 2.0 license.
# Adds comment markers for the relevant file type.
#
# This can also be used to provide a summary of the current situation.
# It will detect the presence of various different license headers.
# Use the -p option for practice mode.
#
# Limitations:
# - Only developed and tested for certain file types. Others will be
# reported and skipped.
# Needs tweaks for other types (see "configuration" section below).
# - Only inserts missing licenses and detects and reports other license types.
#  See ./update-AL20.pl to update to the current license style.
#
# Caveats:
# - As usual, make a backup of your tree first or be prepared to 'svn revert -R'
# your working copy if the script stuffs up.
#
# WARNING: Be sure to look at the output of this script for warnings.
# WARNING: Be sure to do the normal 'svn diff' and review.
# Attend to the warning in tools/copy2license.pl about "collective copyright".
#
# Developed only for UNIX, YMMV.
#
# Procedure:
# See ./relicense.txt for an example procedure.
# Use -p for practise mode.
# Run the script. It will descend the directory tree.
# Run with no parameters or -h to show usage.
#
#=========================================================

use strict;
use vars qw($opt_h $opt_p);
use Getopt::Std;
use File::Basename;
use File::Find;

#--------------------------------------------------
# ensure proper usage
getopts("hp");
if ((scalar @ARGV < 1) || defined($opt_h)) {
  ShowUsage();
  exit;
}

my $startDir = shift;
my $avoidList = shift;
if (!-e $startDir) {
  print STDERR qq!
The start directory '$startDir' does not exist.
!;
  ShowUsage();
  exit;
}
if (defined($avoidList) && !-e $avoidList) {
  print STDERR qq!
The list of files to avoid '$avoidList' does not exist.
!;
  ShowUsage();
  exit;
}
if ($opt_p) { print STDERR "\nDoing practice run. No files will be written\n"; }
print qq!
AL-20 = Apache License 2.0 with original Copyright line.
AL-20a = Apache License 2.0 with original Copyright line and "or its licensors".
AL-20b = Apache License 2.0 with no Copyright line, i.e. the current style.
----------------------

!;

#--------------------------------------------------
# do some configuration
my $license = qq!Licensed to the Apache Software Foundation (ASF) under one or more
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
!;
my @license = split(/\n/, $license);

# build a hash of filename extensions to be processed
# together with the particular style of comment marker to use.
my @xmlFileTypes = (
  ".xml", ".xsl", ".xslt", ".xmap", ".xcat",
  ".xconf", ".xroles", ".roles", ".xsp", ".rss",
  ".xinfo", ".xprofile", ".xsamples", ".xtest", ".xweb", ".xwelcome",
  ".samplesxconf", ".samplesxpipe", ".svg", ".xhtml", ".xhtml2", ".gt", ".jx", ".jmx",
  ".jdo", ".orm", ".jdoquery", ".jelly",
  ".jxt", ".meta", ".pagesheet", ".stx", ".xegrm", ".xgrm", ".xlex", ".xmi",
  ".xsd", ".rng", ".rdf", ".rdfs", ".xul", ".tld", ".xxe", ".ft", ".fv",
  ".wsdd", ".wsdl", ".xlog", ".pom", ".owl",
);
my @sgmlFileTypes = (
  ".dtd", ".mod", ".sgml", ".sgm",
);
my @htmlFileTypes = (
  ".html", ".htm", ".jsp", ".ihtml",
);
my @freemarkerFileTypes = (
  ".ftl",
);
my @cFileTypes = (
  ".java", ".js", ".c", ".h", ".cpp", ".cc", ".cs", ".css", ".egrm", ".grm",
  ".javascript", ".jj", ".gy", ".g",
);
my @shFileTypes = (
  ".sh", ".ccf", ".pl", ".py", ".sed", ".awk",
);
my @propertiesFileTypes = (
  ".properties", ".rnc", ".rnx", ".prefs", ".rb", ".handlers", ".schemas",
);
my @dosFileTypes = (
  ".bat", ".cmd",
);
my @sqlFileTypes = (
  ".script", ".sql",
);
my @vmFileTypes = (
  ".vm",
);
my @ignoreFileTypes = (
  ".txt", ".dcl", ".ent", ".pen", ".project"
);
my (%fileTypes, $fileType);
foreach $fileType (@xmlFileTypes) {
  $fileTypes{$fileType}{type} = "xml";
  $fileTypes{$fileType}{openComment} = "<!--\n";
  $fileTypes{$fileType}{leaderComment} = "  ";
  $fileTypes{$fileType}{closeComment} = "-->\n";
  # insert after line 1 which must be the xml declaration
  $fileTypes{$fileType}{insertionPoint} = "1";
}
foreach $fileType (@sgmlFileTypes) {
  $fileTypes{$fileType}{type} = "sgml";
  $fileTypes{$fileType}{openComment} = "<!--\n";
  $fileTypes{$fileType}{leaderComment} = "  ";
  $fileTypes{$fileType}{closeComment} = "-->\n";
  # insert at very top of file
  $fileTypes{$fileType}{insertionPoint} = "0";
}
foreach $fileType (@htmlFileTypes) {
  $fileTypes{$fileType}{type} = "html";
  $fileTypes{$fileType}{openComment} = "<!--\n";
  $fileTypes{$fileType}{leaderComment} = "  ";
  $fileTypes{$fileType}{closeComment} = "-->\n";
  # insert at very top of file
  $fileTypes{$fileType}{insertionPoint} = "0";
}
foreach $fileType (@freemarkerFileTypes) {
  $fileTypes{$fileType}{type} = "html";
  $fileTypes{$fileType}{openComment} = "<#--\n";
  $fileTypes{$fileType}{leaderComment} = "  ";
  $fileTypes{$fileType}{closeComment} = "-->\n";
  # insert at very top of file
  $fileTypes{$fileType}{insertionPoint} = "0";
}
foreach $fileType (@cFileTypes) {
  $fileTypes{$fileType}{type} = "C";
  $fileTypes{$fileType}{openComment} = "/*\n";
  $fileTypes{$fileType}{leaderComment} = "* ";
  $fileTypes{$fileType}{closeComment} = "*/\n";
  # insert at very top of file
  $fileTypes{$fileType}{insertionPoint} = "0";
}
foreach $fileType (@shFileTypes) {
  $fileTypes{$fileType}{type} = "sh";
  $fileTypes{$fileType}{openComment} = "\n";
  $fileTypes{$fileType}{leaderComment} = "# ";
  $fileTypes{$fileType}{closeComment} = "\n";
  # insert after line 1 which must be #! script invocation
  $fileTypes{$fileType}{insertionPoint} = "1";
}
foreach $fileType (@propertiesFileTypes) {
  $fileTypes{$fileType}{type} = "properties";
  $fileTypes{$fileType}{openComment} = "";
  $fileTypes{$fileType}{leaderComment} = "# ";
  $fileTypes{$fileType}{closeComment} = "\n";
  # insert at very top of file
  $fileTypes{$fileType}{insertionPoint} = "0";
}
foreach $fileType (@dosFileTypes) {
  $fileTypes{$fileType}{type} = "dos";
  $fileTypes{$fileType}{openComment} = "\@echo off\n";
  $fileTypes{$fileType}{leaderComment} = "rem ";
  $fileTypes{$fileType}{closeComment} = "\n";
  # insert at very top of file
  $fileTypes{$fileType}{insertionPoint} = "0";
}
foreach $fileType (@sqlFileTypes) {
  $fileTypes{$fileType}{type} = "sql";
  $fileTypes{$fileType}{openComment} = "";
  $fileTypes{$fileType}{leaderComment} = "-- ";
  $fileTypes{$fileType}{closeComment} = "\n";
  # insert at very top of file
  $fileTypes{$fileType}{insertionPoint} = "0";
}
foreach $fileType (@vmFileTypes) {
  $fileTypes{$fileType}{type} = "vm";
  $fileTypes{$fileType}{openComment} = "#*\n";
  $fileTypes{$fileType}{leaderComment} = "  ";
  $fileTypes{$fileType}{closeComment} = "*#\n";
  # insert after line 1 which must be the xml declaration
  $fileTypes{$fileType}{insertionPoint} = "1";
}

my ($countTotal, $countUnknownType, $countIgnoreType) = (0, 0, 0);
my ($countXmlDeclMissing, $countInserted, $countAvoid) = (0, 0, 0);
my ($countLicense, $countLicense10, $countLicense11, $countLicense12) = (0, 0, 0, 0);
my ($countLicensePD, $countLicenseOther) = (0, 0);
my ($countLicense20, $countLicense20a, $countLicense20b) = (0, 0, 0);

# 3rdParty users of an Apache License
my ($countLicenseF20, $countLicenseF11, $countLicenseF12) = (0, 0, 0);

my $dualLicensesDetected = 0;
my %uniqueSuffixes;
my @avoidList;

# read the avoidList
if (defined($avoidList)) {
  open(INPUT, "<$avoidList") or die "Could not open input file '$avoidList': $!";
  while (<INPUT>) {
    next if (/^#/);
    chomp;
    push(@avoidList, $_);
  }
  close INPUT;
}

#--------------------------------------------------
sub process_file {
  return unless -f && -T; # process only text files
  my $fileName = $File::Find::name;
  my ($file, $dir, $ext) = fileparse($fileName, qr/\.[^.]*/);
  return if ($dir =~ /\/CVS\//); # skip CVS directories
  return if ($dir =~ /\/\.svn\//); # skip SVN directories
  return if ($fileName =~ /.cvsignore/); # skip 
  return if ($file =~ /^\./); # skip hidden files
  foreach my $avoidFn (@avoidList) {
    if ($fileName =~ /$avoidFn/) {
      $countAvoid++;
      return;
    }
  }
  $countTotal++;
  if ($ext eq "") { $ext = "NoExtension"; }
  $uniqueSuffixes{$ext}++;
  print "$fileName, ";
  my $tmpFile = $fileName . ".tmp";
  open(INPUT, "<$fileName") or die "Could not open input file '$fileName': $!";

  # First do some tests on the file to ensure it does not already have a license
  # and ensure that XML files have an xml declaration.
  my ($existsLicense, $warnDualLicense, $existsXmlDecl) = (0, 0, 0);
  my ($warnAL20OldLicense) = 0;
  my ($warnAL20aOldLicense) = 0;
  my $licenseType = "";
  undef $/;  # slurp the whole file
  my $content = <INPUT>;
  # we want our matches to happen only in the top part of the file
# NOTE: You may want to relax this from time-to-time to find
# all possible dual-license issues.
  my $headContent = substr($content, 0, 1500);
  $headContent =~ s/[ \t]+/ /g;

  # detect various existing licenses
  LICENSE_CASE: {
    if ($headContent =~ /Licensed to the Apache Software Foundation \(ASF\) under/) {
      $existsLicense = 1; $countLicense++;
      $countLicense20b++; $licenseType = "AL-20b";
      last LICENSE_CASE;
    }
    if ($headContent =~ /Licensed under the Apache License.*Version 2.0/) {
      $existsLicense = 1; $countLicense++;
      if ($headContent =~ /Apache Software Foundation or its licensors/) {
        $countLicense20a++; $licenseType = "AL-20a";
        $warnAL20aOldLicense = 1;
      }
      else {
        if ($headContent =~ /Copyright.*Apache Software Foundation/) {
          $countLicense20++; $licenseType = "AL-20";
          $warnAL20OldLicense = 1;
        }
        else {
          $countLicenseF20++; $licenseType = "F-AL-20";
        }
      }
      last LICENSE_CASE;
    }
    if ($headContent =~ /The Apache Software License.*Version 1.2/) {
      $existsLicense = 1; $countLicense++;
      if ($headContent =~ /Copyright.*Apache Software Foundation/) {
        $countLicense12++; $licenseType = "AL-12";
      }
      else {
        $countLicenseF12++; $licenseType = "F-AL-12";
      }
      last LICENSE_CASE;
    }
    if ($headContent =~ /The Apache Software License.*Version 1.1/) {
      $existsLicense = 1; $countLicense++;
      if ($headContent =~ /Copyright.*Apache Software Foundation/) {
        $countLicense11++; $licenseType = "AL-11";
      }
      else {
        $countLicenseF11++; $licenseType = "F-AL-11";
      }
      last LICENSE_CASE;
    }
    if ($headContent =~ /Copyright.*The Apache Group/) {
      $countLicense10++; $licenseType = "AL-10";
      $existsLicense = 1; $countLicense++;
      last LICENSE_CASE;
    }
    if ($headContent =~ /Public Domain.*/i) {
      $countLicensePD++; $licenseType = "PublicDomain";
      $existsLicense = 1; $countLicense++;
      last LICENSE_CASE;
    }
    # catchall
    if ($headContent =~ /Copyright|\(c\)/i) {
      # do process xml files that have a copyright attribute
      last LICENSE_CASE if ($headContent =~ /copyright=/i);
      # do process DTD files that have a copyright attribute
      last LICENSE_CASE if ($headContent =~ /copyright CDATA/i);
      # do process css files that have a .copyright section
      last LICENSE_CASE if ($headContent =~ /\.copyright/i);
      # do process files that just talk about copyright
      last LICENSE_CASE if ($headContent =~ /copyright statement/i);
      $countLicenseOther++; $licenseType = "Other";
      $existsLicense = 1; $countLicense++;
      last LICENSE_CASE;
    }
    # catchall
    if ($headContent =~ /re[ -]*distribut/i) {
      $countLicenseOther++; $licenseType = "Other";
      $existsLicense = 1; $countLicense++;
      last LICENSE_CASE;
    }
  }

  # Try to detect if a new AL-20 license has been accidently inserted
  # as well as having some other license.
  # FIXME: If a practice run reveals more types of Foregin copyright
  # then add patterns here.
  if ($licenseType =~ /AL-20/) {
    if (($headContent =~ /Rights Reserved/i) ||
        ($headContent =~ /Public Domain/i) ||
        ($headContent =~ /Copyright.*Copyright/i)) {
      $warnDualLicense = 1; $dualLicensesDetected++;
    }
  }

  # ensure that xml files have an xml declaration
  if ($headContent =~ /^<\?xml/) { $existsXmlDecl = 1; }

  $/ = "\n"; # reset input record separator

  my $recognisedFileType = 0; my $thisFileType = "unknown";
  foreach $fileType (keys %fileTypes) {
    if ($fileType eq $ext) {
      $recognisedFileType = 1;
      $thisFileType = $fileTypes{$fileType}{type};
      last;
    }
  }
  print "extension=$ext, fileType=$thisFileType, ";
  if (!$existsXmlDecl && ($thisFileType eq "xml")) {
    print "XML file does not have XML Declaration so skipping\n";
    $countXmlDeclMissing++;
    return;
  }
  if ($existsLicense) {
    if ($licenseType !~ /^AL/) { print "WARN: "; }
    print "Found existing license (licenseType=$licenseType) so skipping";
    if ($warnAL20OldLicense) { print ", WARN: old AL-20 copyright notice"; }
    if ($warnAL20aOldLicense) { print ", WARN: old AL-20a copyright notice"; }
    if ($warnDualLicense) { print ", WARN: dual license"; }
    print "\n";
    return;
  }
  foreach $fileType (@ignoreFileTypes) {
    if ($fileType eq $ext) {
      $countIgnoreType++;
      print "ignored, ";
    }
  }
  if (!$recognisedFileType) {
    print "File type '$ext' is not recognised so skipping\n";
    $countUnknownType++;
    return;
  }

  # Now process the file.
  my $insertionDone = 0; my ($line, $thisLine);
  if (!$opt_p) {
    open(OUTPUT, ">$tmpFile")
      or die "Could not open output file '$tmpFile': $!";
  }
  $countInserted++;
  if ($fileTypes{$ext}{insertionPoint} == 0) {
    print "Insert new license\n";
    if (!$opt_p) {
      print OUTPUT $fileTypes{$ext}{openComment};
      foreach $line (@license) {
        $thisLine = $fileTypes{$ext}{leaderComment} . $line;
        $thisLine =~ s/\s+$//;
        print OUTPUT $thisLine, "\n";
      }
      print OUTPUT $fileTypes{$ext}{closeComment};
    }
    $insertionDone = 1;
  }
  seek(INPUT, 0, 0); $. = 0; # rewind to top of file
  while (<INPUT>) {
    if (!$opt_p) {
      print OUTPUT $_ or die "Could not write output file '$fileName': $!";
    }
    if (!$insertionDone) {
      if ($. == $fileTypes{$ext}{insertionPoint}) {
        print "Insert new license\n";
        if (!$opt_p) {
          print OUTPUT $fileTypes{$ext}{openComment};
          foreach $line (@license) {
            $thisLine = $fileTypes{$ext}{leaderComment} . $line;
            $thisLine =~ s/\s+$//;
            print OUTPUT $thisLine, "\n";
          }
          print OUTPUT $fileTypes{$ext}{closeComment};
        }
        $insertionDone = 1;
      }
    }
  }
  close INPUT or die "Could not close input file '$fileName': $!";
  if (!$opt_p) {
    close OUTPUT or die "Could not close output file '$tmpFile': $!";
    rename($tmpFile, $fileName);
  }
}
find(\&process_file, $startDir);

#--------------------------------------------------
# Report some statistics
my $statsMsg = "were";
if ($opt_p) { $statsMsg = "would be"; }
$countUnknownType -= $countIgnoreType;
print STDERR qq!
Total $countTotal text files were investigated.
New licenses $statsMsg inserted in $countInserted files.
Skipped $countLicense files with an existing license:
 (Apache v2.0=$countLicense20, v2.0a=$countLicense20a, v2.0b=$countLicense20b)
 (Apache v1.2=$countLicense12, v1.1=$countLicense11, v1.0=$countLicense10)
 (Other=$countLicenseOther, PublicDomain=$countLicensePD)
 (3rdParty using AL v2.0=$countLicenseF20, v1.2=$countLicenseF12, v1.1=$countLicenseF11)
Skipped $countXmlDeclMissing XML files with missing XML Declaration.
!;
if (defined($avoidList)) {
  print STDERR "Avoided $countAvoid files as specified in the avoidList\n";
}
print STDERR qq!
Ignored $countIgnoreType files of specified type (@ignoreFileTypes)
Skipped $countUnknownType files of unknown type.
!;
if ($dualLicensesDetected) {
  print STDERR qq!
WARNING: $dualLicensesDetected files had another license as well as the new
Apache v2.0 license. (Scan the log output for lines with "WARN: dual".)
!;
}
my $suffix;
if ($countUnknownType > 0) {
  print STDERR qq!
List of unknown filename extensions and ignored filename extensions:
(Add new fileTypes to this script if you want them to be catered for.)
!;
  foreach $suffix ( sort keys %uniqueSuffixes) {
    my $suffixKnown = 0;
    foreach $fileType (keys %fileTypes) {
      if ($suffix eq $fileType) { $suffixKnown = 1; }
    }
    if (!$suffixKnown) {
      print STDERR "$suffix=$uniqueSuffixes{$suffix} ";
    }
  }
  print STDERR "\n\n";
}
print STDERR "List of all unique filename extensions:\n";
foreach $suffix ( sort keys %uniqueSuffixes) {
  print STDERR "$suffix=$uniqueSuffixes{$suffix} ";
}
print STDERR "\n\n";
if ($opt_p) { print STDERR "Finished practice run.\n"; }

#==================================================
# ShowUsage
#==================================================

sub ShowUsage {
  print STDERR qq!
Usage: $0 [-h] [-p] startDir [avoidList] > logfile
                                                                                
  where:
  startDir = The SVN directory (pathname) to start processing. Will descend.
  avoidList = List of files and directories to avoid, one per line.

  option:
  h = Show this help message.
  p = Do a practice run. Do not write any files.

!;
}
