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
public class Main {
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
        if(line.hasOption('h')){
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                "java -Xmx{size} -jar org.apache.stanbol.indexing.core-*" +
                "-jar-with-dependencies.jar [options] [configDir]",
                "Indexing Commandline Utility: \n"+
                "  size: Heap requirements depend on the dataset and the configuration.\n"+
                "        1024m should be a reasonable default.",
                options,
                null);
        System.exit(0);
        }
        Indexer indexer;
        IndexerFactory factory = IndexerFactory.getInstance();
        if(args.length > 0){
            indexer = factory.create(args[0]);
        } else {
            indexer = factory.create();
        }
        indexer.index();
    }

}
