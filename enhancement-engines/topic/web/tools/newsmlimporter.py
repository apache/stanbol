#!/usr/bin/env python
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
"""Basic python script to load NewsML documents as training set

Need Python 2.7 and lxml.

TODO: port to Python 3 as well if not working by default.
"""
from __future__ import print_function

import os
from time import time
from lxml import html
from lxml import etree
from urllib import quote
import urllib2
from hashlib import sha1


IPTC_SUBJECT_PREFIX = "http://cv.iptc.org/newscodes/subjectcode/"


def find_text_and_subjects(newsml_content,
                           subject_tags=('SubjectMatter', 'SubjectDetail'),
                           text_tags=('HeadLine',),
                           html_tags=('body.content',)):
    # First parse of the document as XML for the structured attributes
    xtree = etree.ElementTree(etree.fromstring(newsml_content))
    text_items = [e.text.strip()
                  for tag in text_tags
                  for e in xtree.findall('//' + tag)]
    subjects = [IPTC_SUBJECT_PREFIX + e.get('FormalName')
                for tag in subject_tags
                for e in xtree.findall('//' + tag)]

    # Then use HTML parser to find the that looks like HTML hence can leverage
    # the text_content method.
    htree = etree.ElementTree(html.document_fromstring(newsml_content))

    text_items += [e.text_content().strip()
                   for tag in html_tags
                   for e in htree.findall('//' + tag)]
    text = "\n\n".join(text_items)
    return text, subjects


def register_newsml_document(text, codes, url):
    id = sha1(text.encode('utf-8')).hexdigest()
    url += "?example_id=%s" % id
    for code in codes:
        url += "&concept=%s" % quote(code)
    request = urllib2.Request(url, data=text.encode('utf-8'))
    request.add_header('Content-Type', 'text/plain')
    opener = urllib2.build_opener()
    opener.open(request).read()


def print_newsml_summary(text, codes, server_url=None):
    print(text.split('\n\n')[0])
    for code in codes:
        print('code: ' + code)
    print()


if __name__ == "__main__":
    import sys

    # TODO: use argparse and debug switch to use print_newsfile_summary
    # instead of the default handler
    topfolder = sys.argv[1]
    max = int(sys.argv[2])
    server_url = sys.argv[3]
    handle_news = register_newsml_document

    count = 0
    previous = time()
    for dirpath, dirnames, filenames in os.walk(topfolder):
        if count >= max:
            break

        if '.svn' in dirnames:
            dirnames.remove('.svn')

        for filename in filenames:
            if count >= max:
                break
            if not filename.endswith('.xml'):
                continue
            full_path = os.path.join(topfolder, dirpath, filename)
            newsml_content = open(full_path, 'rb').read()
            text, codes = find_text_and_subjects(newsml_content)
            if len(codes) == 0:
                # ignore document without subject info
                continue
            handle_news(text, codes, server_url)
            count += 1
            if count % 100 == 0:
                delta, previous = time() - previous, time()
                print("Processed news %03d/%03d in %06.3fs"
                      % (count, max, delta))
