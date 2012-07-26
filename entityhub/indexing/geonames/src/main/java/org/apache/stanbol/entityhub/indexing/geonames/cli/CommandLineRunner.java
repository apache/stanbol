/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.entityhub.indexing.geonames.cli;

import static org.apache.stanbol.entityhub.indexing.geonames.GeoNamesIndexer.KEY_CHUNK_SIZE;
import static org.apache.stanbol.entityhub.indexing.geonames.GeoNamesIndexer.KEY_DATA_DIR;
import static org.apache.stanbol.entityhub.indexing.geonames.GeoNamesIndexer.KEY_GEONAMES_ARCHIVE;
import static org.apache.stanbol.entityhub.indexing.geonames.GeoNamesIndexer.KEY_GEONAMES_ONTOLOGY;
import static org.apache.stanbol.entityhub.indexing.geonames.GeoNamesIndexer.KEY_INDEX_ONTOLOGY_STATE;
import static org.apache.stanbol.entityhub.indexing.geonames.GeoNamesIndexer.KEY_START_INDEX;
import static org.apache.stanbol.entityhub.indexing.geonames.GeoNamesIndexer.KEY_YARD;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.stanbol.entityhub.indexing.geonames.GeoNamesIndexer;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class CommandLineRunner {
    private CommandLineRunner(){}

    protected static final Logger log = LoggerFactory.getLogger(CommandLineRunner.class);

    private static final String header;
    static {
        StringBuilder builder = new StringBuilder();
        builder.append("Description:\nThis Utility creates a full Yard for geonames.org by using the SolrYard implementation.\n");
        builder.append("\nParameter:\n");
        builder.append(" - \"-Xmx\": This implementation loads alternate labels into memory. Therefore it needs a lot of memory during indexing. Parse at least \"-Xmx1024M\" to provide 1GByte memory to the Java Vm. In case of OutOfMemory errors you need to increase this value!");
        builder.append(" - solrServerUri : The URL of the Solr Server used to index the data. Make sure to use the schema.xml as needed by the SolrYard!\n");
        builder.append(" - geonamesDataDumpDir: The relative or absolute path to the Dir with the geonames.org data required for indexing\n");
        builder.append("\nOptions:\n");
        header = builder.toString();
        builder = null;
    }
    private static final Options options;
    static {
        options = new Options();
        options.addOption("h", "help", false, "display this help and exit");
        options.addOption("d", "debug", false, "show debug stacktrace upon error");
        //options.addOption("yt","yardtype",false, "the type of the yard used as target 'solr' or 'rdf' (default:'solr')");
        //options.addOption("i","index",true, "Base URI of the used Solr Server used to index the data");
        options.addOption("n", "name", true, "the id and name used for the Yard (default: 'geonames')");
        options.addOption("a","archive",true, "file name of the archive within the data directory (default: 'allCountries.zip')");
        options.addOption("o","ontology",true, "file name of the ontology within the data directory (default: 'ontology_v2.2.1.rdf')");
        options.addOption("io","indexOnt",false, "index also the geonames ontology");
        options.addOption("c","chunksize",true, "the number of documents stored in one chunk (default: 1000");
        options.addOption("s","start",true, "the line number of the geonames table to start(default: 0");
    }
    private static final String footer;
    static {
        StringBuilder builder = new StringBuilder();
        builder.append("Required data:\n");
        builder.append(" - archive with the toponyms (default 'allCountries.zip', see option 'a'\n");
        builder.append(" - countryInfo.txt : additional infos for country codes\n");
        builder.append(" - admin1CodesASCII.txt : leval 1 administrative regions\n");
        builder.append(" - admin2Codes.txt: Level 2 administrative regions\n");
        builder.append(" - alternateNames.zip or .txt: names of features in different languages\n");
        builder.append(" - geonames ontology: only needed if '-io' (default 'ontology_v2.2.1.rdf', see option 'o')\n");
        footer = builder.toString();
        builder = null;
    }
    public static void main(String[] args) throws IOException, ParseException, YardException {
        CommandLineParser parser = new PosixParser();
        CommandLine line = parser.parse(options, args);
        args = line.getArgs();

        if (line.getArgs().length < 2 || line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "java -Xmx1024M -jar org.apache.stanbol.indexing.geonames-0.1-SNAPSHOT-jar-with-dependencies.jar [options] solrServerUri geonamesDataDumpDir",
                    header,
                    options,
                    footer);
            System.exit(0);
        }
        String yardName = line.getOptionValue("n");
        if(yardName == null){
            yardName = "geonames";
        }
        SolrYardConfig yardConfig = new SolrYardConfig(yardName, line.getArgs()[0]);
        Dictionary<String, Object> indexingConfig = new Hashtable<String, Object>();
        SolrYard yard = new SolrYard(yardConfig);
        indexingConfig.put(KEY_YARD, yard);
        indexingConfig.put(KEY_DATA_DIR, line.getArgs()[1]);
        indexingConfig.put(KEY_INDEX_ONTOLOGY_STATE, line.hasOption("io"));
        indexingConfig.put(KEY_GEONAMES_ONTOLOGY,
                line.getOptionValue("o", "ontology_v2.2.1.rdf"));
        indexingConfig.put(KEY_GEONAMES_ARCHIVE,
                line.getOptionValue("a","allCountries.zip"));
        Long start;
        try {
            start = Long.valueOf(line.getOptionValue("s", "0"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value for option \"start\" need to be a valid Integer");
        }
        if(start<0){
            log.warn("Negative number parsed for option \"start\". Use '0' as default.");
            start = 0l;
        }
        indexingConfig.put(KEY_START_INDEX, start);
        Integer chunkSize;
        try {
            chunkSize = Integer.valueOf(line.getOptionValue("c", "1000"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value for option \"chunkSize\" need to be a valid Integer");
        }
        if(chunkSize<0){
            log.warn("Negative number parsed for option \"chunkSize\". Use '1000' as default.");
            chunkSize = 1000;
        }
        indexingConfig.put(KEY_CHUNK_SIZE, chunkSize);
        GeoNamesIndexer indexer = new GeoNamesIndexer(indexingConfig);
        indexer.index();
    }



}
