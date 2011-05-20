This directory should hold the RDF Dump available at
http://dblp.l3s.de/dblp.rdf.gz

NOTE that you need to rename the file to from "dblp.rdf.gz" to "dblp.nt.gz"
because it is encoded using N-Triples and not rdf/xml as the file extension
indicated.

You can execute the following two commands within this directory to get the
required file

curl -C - -O http://dblp.l3s.de/dblp.rdf.gz
mv dblp.rdf.gz dblp.nt.gz