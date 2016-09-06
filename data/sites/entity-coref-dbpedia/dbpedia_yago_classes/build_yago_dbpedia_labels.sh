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

#!/usr/bin/env bash

YAGO=http://resources.mpi-inf.mpg.de/yago-naga/yago/download/yago/

files=(yagoLabels.ttl.7z \
	yagoDBpediaClasses.ttl.7z
    )
YAGO_LABELS=yagoLabels.ttl
YAGO_DBPEDIA_CLASSES=yagoDBpediaClasses.ttl
YAGO_WORDNET_LABELS=yago_wordnet_labels
YAGO_WORDNET_DBPEDIA_CLASSES=yago_wordnet_dbpedia_classes
DBPEDIA_YAGO_CLASS_LABELS_NT=dbpedia_yago_classes_labels.nt

# First, download and decompress the necessary yago files.
for i in "${files[@]}"
do
    :
		if [ ! -f ${i} ]; 
		then
			url=${YAGO}/${i}
			wget -c ${url}
		fi
		
		echo "Unzipping ${i}"
		7za e ${i}
done

# Second, create a file with <wordnet_class> rdfs:label "label" format.
grep '^<wordnet_' ${YAGO_LABELS} | grep 'rdfs:label' > ${YAGO_WORDNET_LABELS}

# Third, create a file with wordnet to dbpedia yago class mappings.
grep '^<wordnet_' ${YAGO_DBPEDIA_CLASSES} > ${YAGO_WORDNET_DBPEDIA_CLASSES}

# Last, create the nt file which will contain the dbpedia yago class and its labels.
touch ${DBPEDIA_YAGO_CLASS_LABELS_NT};

YAGO_LABELS_NUM_LINES=$(wc -l < ${YAGO_WORDNET_LABELS})
PROCESSED_LINES=0
NOT_FOUND_LINES=0

echo -e "\n\nStarting to process $YAGO_LABELS_NUM_LINES lines"

while read line
do
	wordnet_class=`echo $line | awk '{print $1}'`;
	dbpedia_class=`grep $wordnet_class $YAGO_WORDNET_DBPEDIA_CLASSES | awk '{split($0,a," "); print a[3]}'`;
	
	if [ -z "$dbpedia_class" ]
	then
		((NOT_FOUND_LINES++));
		continue;
	fi	
	
	mapped_line=${line/$wordnet_class/$dbpedia_class};
	mapped_line_with_label=${mapped_line/rdfs:label/<http://www.w3.org/2000/01/rdf-schema#label>};
	mapped_line_with_label_lang=${mapped_line_with_label/@eng/@en};
	
	echo $mapped_line_with_label_lang >> ${DBPEDIA_YAGO_CLASS_LABELS_NT};
	
	((PROCESSED_LINES++));
	echo -ne "$PROCESSED_LINES/$YAGO_LABELS_NUM_LINES processed\r";
done < ${YAGO_WORDNET_LABELS}

echo -e "\nDone processing lines. Skipped $NOT_FOUND_LINES not found dbpedia classes. Creating .nt archive."
bzip2 ${DBPEDIA_YAGO_CLASS_LABELS_NT}

# Cleanup
rm ${YAGO_LABELS}
rm ${YAGO_DBPEDIA_CLASSES}
rm ${YAGO_WORDNET_LABELS}
rm ${YAGO_WORDNET_DBPEDIA_CLASSES}
