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
package org.apache.stanbol.entityhub.indexing;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.stanbol.entityhub.indexing.core.Indexer;
import org.apache.stanbol.entityhub.indexing.core.IndexerFactory;

/**
 * Command Line Utility for indexing. If not other specified the configuration
 * is expected under {workingdir}/indexing.
 * @author Rupert Westenthaler
 *
 */
public final class Main {

    /**
     * Restrict instantiation
     */
    private Main() {}

    private static final Options options;
    static {
        options = new Options();
        options.addOption("h", "help", false, "display this help and exit");
        options.addOption("c","chunksize",true, 
            String.format("the number of documents stored to the Yard in one chunk (default: %s)",
                Indexer.DEFAULT_CHUNK_SIZE));
    }
    /**
     * @param args
     * @throws ParseException 
     */
    public static void main(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine line = parser.parse(options, args);
        args = line.getArgs();
        if(line.hasOption('h') || args.length <= 0){
            printHelp();
            System.exit(0);
        }
        Indexer indexer;
        IndexerFactory factory = IndexerFactory.getInstance();
        String path = null;
        if(args.length > 1){
            path = args[1];
        }
        if("init".equalsIgnoreCase(args[0]) ||
                "index".equalsIgnoreCase(args[0]) || 
                "postprocess".equalsIgnoreCase(args[0]) ||
                "finalise".equalsIgnoreCase(args[0])){
            if(path != null){
                indexer = factory.create(path);
            } else {
                indexer = factory.create();
            }
            if(line.hasOption('c')){
                int cunckSize = Integer.parseInt(line.getOptionValue('c'));
                indexer.setChunkSize(cunckSize);
            }
            if("index".equalsIgnoreCase(args[0])){
                indexer.index();
            } else if("postprocess".equalsIgnoreCase(args[0])){
                indexer.initialiseIndexing();
                indexer.skipIndexEntities();
                indexer.postProcessEntities();
                indexer.finaliseIndexing();
            } else if ("finalise".equalsIgnoreCase(args[0])){
                indexer.initialiseIndexing();
                indexer.skipIndexEntities();
                indexer.skipPostProcessEntities();
                indexer.finaliseIndexing();
            }
        } else {
            System.err.println("Unknown command "+args[0]+" (supported: init,index)\n\n");
            printHelp();
        }
        System.exit(0);
    }
    /**
     * 
     */
    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
            "java -Xmx{size} -jar org.apache.stanbol.indexing.core-*" +
            "-jar-with-dependencies.jar [options] init|index [configDir]",
            "Indexing Commandline Utility: \n"+
            "  size:        Heap requirements depend on the dataset and the\n"+
            "               configuration. 1024m should be a reasonable default.\n" +
            "  init:        Initialise the configuration with the defaults \n" +
            "  index:       Needed to start the indexing process\n" +
            "  postprocess: Skip indexing and directly start with post-processing\n" +
            "  finalise:    Skip indexing and post-processing; only finalises \n" +
            "               the index. \n"+
            "  configDir: the path to the configuration directory (default:" +
            " user.dir)",
            options,
            null);
    }

}
