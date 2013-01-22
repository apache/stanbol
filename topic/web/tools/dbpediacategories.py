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
""""Build a classifier using a subset of the DBpedia categories"""
from __future__ import print_function

from bz2 import BZ2File
from time import time
import urllib2
from urllib import quote

DBPEDIA_URL_PREFIX = "http://dbpedia.org/resource/"


def load_topics_from_tsv(filename, server_url):
    lines = open(filename, 'rb').readlines()

    count = 0
    previous = time()

    for line in lines:
        concept, broader_concepts, primary_topic = line.split('\t')
        primary_topic = DBPEDIA_URL_PREFIX + primary_topic.strip()
        concept = DBPEDIA_URL_PREFIX + concept.strip()
        if broader_concepts == '\\N':
            # postgresql marker for NULL values in TSV files
            broader_concepts = []
        else:
            broader_concepts = [DBPEDIA_URL_PREFIX + b.strip()
                                for b in broader_concepts.split()]

        url = server_url + "?id=%s&primary_topic=%s" % (
            concept, primary_topic)

        for broader_concept in broader_concepts:
            url += "&broader=%s" % quote(broader_concept)

        # force POST verb with data keyword
        request = urllib2.Request(url, data="")
        opener = urllib2.build_opener()
        opener.open(request).read()

        count += 1
        if count % 1000 == 0:
            delta, previous = time() - previous, time()
            print("Imported concepts %03d/%03d in %06.3fs"
                  % (count, len(lines), delta))


def load_examples_from_tsv(filename, server_url):
    if filename.endswith('.bz2'):
        lines = BZ2File(filename).readlines()
    else:
        lines = open(filename, 'rb').readlines()

    count = 0
    previous = time()

    for line in lines:
        example_id, categories, text = line.split('\t')
        example_id = DBPEDIA_URL_PREFIX + example_id
        categories = [DBPEDIA_URL_PREFIX + c for c in categories.split()]

        url = server_url + "?example_id=%s" % example_id
        for category in categories:
            url += "&concept=%s" % quote(category)
        request = urllib2.Request(url, data=text)
        request.add_header('Content-Type', 'text/plain')
        opener = urllib2.build_opener()
        opener.open(request).read()

        count += 1
        if count % 1000 == 0:
            delta, previous = time() - previous, time()
            print("Processed articles %03d/%03d in %06.3fs"
                  % (count, len(lines), delta))


if __name__ == "__main__":
    import sys
    topics_filename = sys.argv[1]
    examples_filename = sys.argv[2]
    topic_model_url = sys.argv[3]

    print("Loading taxonomy definition from:", topics_filename)
    t0 = time()
    load_topics_from_tsv(topics_filename,
                         topic_model_url + '/concept')
    print("Taxonomy loaded in %0.3fs." % (time() - t0))

    print("Loading training set from:", examples_filename)
    t0 = time()
    load_examples_from_tsv(examples_filename,
                           topic_model_url + '/trainingset')
    print("Dataset loaded in %0.3fs." % (time() - t0))

    print("Training model from dataset...")
    # Force usage of the POST HTTP verb:
    t0 = time()
    request = urllib2.Request(topic_model_url + '/trainer', data="")
    opener = urllib2.build_opener().open(request).read()
    print("Model updated in %0.3fs." % (time() - t0))
