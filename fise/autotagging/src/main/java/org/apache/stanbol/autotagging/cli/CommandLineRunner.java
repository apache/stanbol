package org.apache.stanbol.autotagging.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.stanbol.autotagging.Autotagger;
import org.apache.stanbol.autotagging.TagInfo;
import org.apache.stanbol.autotagging.jena.ModelIndexer;
import org.apache.stanbol.autotagging.jena.ModelResampler;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;


/**
 * Command line User Interface for importing RDF data into Jena models from
 * dumps, sampling the relevant part and indexing the results with Lucene.
 *
 * @author ogrisel
 */
public class CommandLineRunner {

    public static Options makeCommonOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "display this help and exit");
        options.addOption("d", "debug", false,
                "show debug stacktrace upon error");
        return options;
    }

    public static void handleModel(String[] args) throws ParseException,
            IOException {
        CommandLineParser parser = new PosixParser();
        Options options = makeCommonOptions();
        CommandLine line = parser.parse(options, args);
        args = line.getArgs();

        if (args.length < 2 || line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "model /path/to/tdb-model file.nt [file2.n3.gz file3.xml.bz2 ...]",
                    options);
            System.exit(0);
        }
        String modelPath = args[1];
        Model model = TDBFactory.createModel(modelPath);
        for (String filename : Arrays.asList(args).subList(2, args.length)) {
            System.out.printf("loading '%s' into model '%s'...", filename,
                    modelPath);
            InputStream is = new FileInputStream(filename);

            if (filename.endsWith(".gz")) {
                is = new GZIPInputStream(is);
                filename = filename.replaceFirst("\\.gz$", "");
            } else if (filename.endsWith(".bz2")) {
                is = new BZip2CompressorInputStream(is);
                filename = filename.replaceFirst("\\.bz2$", "");
            }

            String format = null;
            if (filename.endsWith(".nt")) {
                format = "N-TRIPLE";
            } else if (filename.endsWith(".n3")) {
                format = "N3";
            } // XML is the default format

            model.read(is, null, format);
            System.out.println(" done");
        }
    }

    public static void handleResample(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        Options options = makeCommonOptions();
        Option maxTopResourcesOpt = new Option("t", "max-top-resources", true,
                "maximum number of resources to sample");
        maxTopResourcesOpt.setType(Integer.class);
        options.addOption(maxTopResourcesOpt);
        Option scoreFileOpt = new Option("s", "score-file", true,
                "use TSV file holding ranked and scored resources");
        options.addOption(scoreFileOpt);
        CommandLine line = parser.parse(options, args);
        boolean debug = line.hasOption("d");
        args = line.getArgs();
        if (args.length != 2 || line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "resample /path/to/src-tdb-model /path/to/sampled-tdb-model",
                    options);
            System.exit(0);
        }
        try {
            int maxTopResources = Integer.parseInt(line.getOptionValue("t",
                    "10000"));
            String scores = line.getOptionValue("s");
            ModelResampler.resample(new File(args[0]), new File(args[1]),
                    new File(scores), maxTopResources);
        } catch (Exception e) {
            System.err.println(String.format("ERROR: %s - %s",
                    e.getClass().getSimpleName(), e.getMessage()));
            if (debug) {
                e.printStackTrace();
            }
            System.exit(5);
        }
    }

    public static void handleIndex(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        Options options = makeCommonOptions();
        CommandLine line = parser.parse(options, args);
        args = line.getArgs();
        if (args.length < 2 || line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "index /path/to/tdb-model /path/to/lucene-index", options);
            System.exit(0);
        }
        try {
            ModelIndexer.index(new File(args[0]), new File(args[1]));
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(4);
        }
    }

    public static void handleSuggest(String[] args) throws IOException,
            ParseException {
        CommandLineParser parser = new PosixParser();
        Options options = makeCommonOptions();

        options.addOption("i", "index", true,
                "path to a specific lucene directory");

        options.addOption("n", "name", true,
                "restrict suggestions to lookup entities matching the provided name");

        options.addOption("c", "context", true,
                "restrict suggestions to entities similar to the provided context");

        options.addOption("f", "context-file", true,
                "restrict suggestions to entities similar to the provided utf-8 text file");

        options.addOption("t", "type", true,
                "restrict suggestions to entities of given type");

        Option maxSuggestionsOpt = new Option("s", "max-suggestions", true,
                "maximum number of suggestions");
        maxSuggestionsOpt.setType(Integer.class);
        options.addOption(maxSuggestionsOpt);

        CommandLine line = parser.parse(options, args);
        args = line.getArgs();
        String name = line.getOptionValue("n");
        String context = line.getOptionValue("c", "");
        String contextFile = line.getOptionValue("f");

        if (line.hasOption("h")
                || (name == null && context == null && contextFile == null)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "suggest --name \"John Smith\" --context-file smith-biography.txt ",
                    options);
            System.exit(0);
        }

        String customIndex = line.getOptionValue("i");
        Directory dir;
        if (customIndex != null) {
            dir = FSDirectory.open(new File(customIndex));
        } else {
            dir = FSDirectory.open(ModelIndexer.buildDefaultIndex());
        }

        int maxSuggestions = Integer.parseInt(line.getOptionValue("s", "3"));
        Autotagger tagger = new Autotagger(dir).withMaxSuggestions(maxSuggestions);

        if (contextFile != null) {
            context += " ";
            context = IOUtils.toString(new FileInputStream(
                    new File(contextFile)));
        }
        String type = line.getOptionValue("t");

        System.out.printf("Computing suggestions...");
        long startTime = System.currentTimeMillis();
        List<TagInfo> suggestions = tagger.suggestForType(name, context, type);
        System.out.printf(" done in %dms:\n",
                (System.currentTimeMillis() - startTime));

        for (int i = 0; i < suggestions.size(); i++) {
            TagInfo tag = suggestions.get(i);
            System.out.printf("Suggestion #%d (score: %f): '%s'\n", i + 1,
                    tag.getConfidence(), tag.getLabel());
            System.out.printf("URI:\t%s\n", tag.getId());
            for (String tagType : tag.getType()) {
                System.out.printf("type:\t%s\n", tagType);
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        if (args.length < 1) {
            System.out.println("expected command: model, resample, index or suggest");
            System.exit(1);
        }

        String command = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        if (command.equals("model")) {
            handleModel(commandArgs);
        } else if (command.equals("resample")) {
            handleResample(commandArgs);
        } else if (command.equals("index")) {
            handleIndex(commandArgs);
        } else if (command.equals("suggest")) {
            handleSuggest(commandArgs);
        } else {
            System.err.append("Unknown command: " + args[0]);
            System.exit(5);
        }
    }

}
