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
        url=${YAGO}/${i}
        wget -c ${url}
		7za e ${i}
		rm ${i}
done

# Second, create a file with <wordnet_class> rdfs:label "label" format.
grep '^<wordnet_' ${YAGO_LABELS} | grep 'rdfs:label' > ${YAGO_WORDNET_LABELS}

# Third, create a file with wordnet to dbpedia yago class mappings.
grep '^<wordnet_' ${YAGO_DBPEDIA_CLASSES} > ${YAGO_WORDNET_DBPEDIA_CLASSES}

# Last, create the nt file which will contain the dbpedia yago class and its labels.
touch ${DBPEDIA_YAGO_CLASS_LABELS_NT};

while read line
do
	wordnet_class=`echo $line | awk '{print $1}'`;
	dbpedia_class=`grep $wordnet_class $YAGO_WORDNET_DBPEDIA_CLASSES | awk '{split($0,a," "); print a[3]}'`;
	
	if [ -z "$dbpedia_class" ]
	then
		continue;
	fi	
	
	mapped_line=${line/$wordnet_class/$dbpedia_class};
	mapped_line_with_label=${mapped_line/rdfs:label/<http://www.w3.org/2000/01/rdf-schema#label>};
	mapped_line_with_label_lang=${mapped_line_with_label/@eng/@en};
	
	echo "Mapping $wordnet_class to $dbpedia_class";
	
	echo $mapped_line_with_label_lang >> ${DBPEDIA_YAGO_CLASS_LABELS_NT};
done < ${YAGO_WORDNET_LABELS}

bzip2 ${DBPEDIA_YAGO_CLASS_LABELS_NT}

# Cleanup
rm ${YAGO_LABELS}
rm ${YAGO_DBPEDIA_CLASSES}
rm ${YAGO_WORDNET_LABELS}
rm ${YAGO_WORDNET_DBPEDIA_CLASSES}