package eu.iksproject.rick.indexing.dbPedia.cli;

import static eu.iksproject.rick.indexing.rdf.RdfIndexer.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.rick.indexing.rdf.RdfIndexer;
import eu.iksproject.rick.servicesapi.defaults.NamespaceEnum;
import eu.iksproject.rick.servicesapi.model.rdf.RdfResourceEnum;
import eu.iksproject.rick.servicesapi.yard.YardException;
import eu.iksproject.rick.yard.solr.impl.SolrYard;
import eu.iksproject.rick.yard.solr.impl.SolrYardConfig;

public class CommandLineRunner {
    public static final String[] defaultFieldMappings = new String [] {
        // --- Define the Languages for all fields ---
        //NOTE: the leading space is required for the global filter!
        // --- RDF, RDFS and OWL Mappings ---
        "rdfs:label", //rdf:label
        "rdfs:comment",//rdf:comment
        "rdf:type | d=rick:ref",//The types
        "owl:sameAs | d=rick:ref",//used by LOD to link to URIs used to identify the same Entity
        // --- Dublin Core ---
        "dc:*", //all DC Terms properties
        "dc-elements:*", //all DC Elements (one could also define the mappings to the DC Terms counterparts here
        // --- Spatial Things ---
        "geo:lat | d=xsd:double",
        "geo:long | d=xsd:double",
        "geo:alt | d=xsd:int;xsd:float", //also allow floating point if one needs to use fractions of meters
        // --- Thesaurus (via SKOS) ---
        //SKOS can be used to define hierarchical terminologies
        "skos:*",
        "skos:broader | d=rick:ref",
        "skos:narrower | d=rick:ref",
        "skos:related | d=rick:ref",
        "skos:member | d=rick:ref",
        "skos:subject | d=rick:ref",
        "skos:inScheme | d=rick:ref",
        "skos:hasTopConcept | d=rick:ref",
        "skos:topConceptOf | d=rick:ref",
        // --- Social Networks (via foaf) ---
        "foaf:*", //The Friend of a Friend schema often used to describe social relations between people
        "foaf:knows | d=rick:ref",
        "foaf:made | d=rick:ref",
        "foaf:maker | d=rick:ref",
        "foaf:member | d=rick:ref",
        "foaf:homepage | d=xsd:anyURI",
        "foaf:depiction | d=xsd:anyURI",
        "foaf:img | d=xsd:anyURI",
        "foaf:logo | d=xsd:anyURI",
        "foaf:page | d=xsd:anyURI", //page about the entity
        // --- dbPedia specific
        "dbp-ont:*",
        "dbp-ont:thumbnail | d=xsd:anyURI > foaf:depiction",
//        "dbp-prop:latitude | d=xsd:decimal > geo:lat",
//        "dbp-prop:longitude | d=xsd:decimal > geo:long",
        "dbp-prop:population | d=xsd:integer",
        "dbp-prop:website | d=xsd:anyURI > foaf:homepage"
    };
    public static final Map<String,Float> fieldBoosts;
    static {
        Map<String,Float> boosts = new HashMap<String, Float>();
        boosts.put(NamespaceEnum.rdfs+"label", 3f);
        boosts.put(NamespaceEnum.dcTerms+"title", 3f);
        boosts.put(NamespaceEnum.dcElements+"title", 3f);
        boosts.put(NamespaceEnum.foaf+"name", 3f);
        boosts.put(NamespaceEnum.skos+"prefLabel", 3f);
        boosts.put(NamespaceEnum.skos+"altLabel", 2f);
        fieldBoosts = Collections.unmodifiableMap(boosts);
    }

    protected static final Logger log = LoggerFactory.getLogger(CommandLineRunner.class);

    private static final String header;
    static {
        StringBuilder builder = new StringBuilder();
        builder.append("Description:\nThis Utility creates a full Yard for dbPedia.org by using the SolrYard implementation.\n");
        builder.append("\nParameter:\n");
        builder.append(" - \"-Xmx\": This implementation does not need much memory. RDF data are loaded into the file based Jena TDB store. Indexing is done in chunks of 1000 (default). In case of OutOfMemory errors you need to increase this value!");
        builder.append(" - solrServerUri : The URL of the Solr Server used to index the data. Make sure to use the schema.xml as needed by the SolrYard!\n");
        builder.append(" - dbPediaDumpDir: The relative or absolute path to the Dir with the source RDF files to be used for indexing. You can direcly use the compressed archives. All files in that directory are used to create the index\n");
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
        options.addOption("n", "name", true, "the id and name used for the Yard (default: 'dbPedia')");
        options.addOption("m","mappings",true, "file with the fieldMappings used for indexing (this will replace the defaults)");
        options.addOption("c","chunksize",true, "the number of documents stored in one chunk (default: 1000");
        options.addOption("s","skipRdf",false, "this options allow to skip the loading of the RDF Data (e.g. if alredy loaded to the Triple Store)");
        options.addOption("i","incomming",true,"the file with the incomming links for Entities (id tab num, highest num needs to be the first line!)");
        options.addOption("ri","requiredIncomming",true,"the minimum number of incomming lins for Entities to be indexed");
        options.addOption("r","resume",true,"resume a previous canceled indexing session (usually used with -s)");
    }
    private static final String footer;
    static {
        StringBuilder builder = new StringBuilder();
        builder.append("Default Field Mappings:\n");
        for(String mapping: defaultFieldMappings){
            builder.append(String.format("\t%s",mapping));
        }
        footer = builder.toString();
        builder = null;
    }

    private static float minRequiredRanking;

    public static void main(String[] args) throws IOException, ParseException, YardException {
        CommandLineParser parser = new PosixParser();
        CommandLine line = parser.parse(options, args);
        args = line.getArgs();

        if (line.getArgs().length < 2 || line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "java -Xmx1024M -jar eu.iksproject.indexing.dbPedia-0.1-SNAPSHOT-jar-with-dependencies.jar [options] solrServerUri dbPediaDataDumpDir",
                    header,
                    options,
                    footer);
            System.exit(0);
        }
        String yardName = line.getOptionValue("n");
        if(yardName == null){
            yardName = "dbPedia";
        }
        Dictionary<String, Object> indexingConfig = new Hashtable<String, Object>();
        //first the SolrServer used to store the index
        URL solrServer = new URL(line.getArgs()[0]);
        SolrYardConfig yardConfig = new SolrYardConfig(yardName, solrServer);
        //use the signRank as default for document Boosts
        yardConfig.setDocumentBoostFieldName(RdfResourceEnum.signRank.getUri());
        //increase the boost for fields that are usually used as labels
        yardConfig.setFieldBoosts(fieldBoosts);
        SolrYard yard = new SolrYard(yardConfig);
        indexingConfig.put(KEY_YARD, yard);
        //now the other properties
        File dataDir = new File(line.getArgs()[1]);
        if(!dataDir.exists()){
            log.error("Parsed Data Directory "+dataDir+" does not Exist on the File System");
            System.exit(0);
        }
        if(!dataDir.isDirectory()){
            log.error("Parsed Data Directory "+dataDir+" exists, but is not a Directory!");
            System.exit(0);
        }
        if(!dataDir.canRead()){
            log.error("Unable to read Data Directory "+dataDir+"!");
            System.exit(0);
        }
        File[] files = dataDir.listFiles();
        indexingConfig.put(KEY_RDF_FILES, Arrays.asList(files));
        indexingConfig.put(KEY_RDF_STORE_DIR, "dbPedia-rdf-data");
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
        if(!line.hasOption("m")){
            indexingConfig.put(KEY_FIELD_MAPPINGS, Arrays.asList(defaultFieldMappings));
        } else {
            File fieldMappingFile = new File(line.getOptionValue("m"));
            if(fieldMappingFile.exists() && fieldMappingFile.isFile() && fieldMappingFile.canRead()){
                String[] fieldMappings = IOUtils.toString(new FileInputStream(fieldMappingFile)).split("\n");
                indexingConfig.put(KEY_FIELD_MAPPINGS, Arrays.asList(fieldMappings));
            } else {
                log.error(String.format("Invalid fieldMapping File (exist: %s | isFile: %s | canRead: %s)",
                        fieldMappingFile.exists(),fieldMappingFile.isFile(),fieldMappingFile.canRead()));
                System.exit(0);
            }

        }
        if(line.hasOption("s")){
            indexingConfig.put(KEY_SKIP_READ, Boolean.TRUE);
        }
        indexingConfig.put(KEY_CHUNK_SIZE, chunkSize);
        indexingConfig.put(KEY_MODEL_NAME, "indexingModel-49e176b9-0138-dd4c-2b87-89af85b89a57");
        //entityRank related properties
        if(line.hasOption("i")){
            File tsvScoreFile = new File(line.getOptionValue("i"));
            if(tsvScoreFile.exists() && tsvScoreFile.isFile() && tsvScoreFile.canRead()){
                int minIncommings = -1;
                try {
                    minIncommings = Integer.parseInt(line.getOptionValue("ri", "-1"));
                } catch (Exception e) {
                    log.error("Value of option --minIncomming/-mi MUST BE a valid integer");
                    System.exit(0);
                }
                Map<String,Float> entityRankings = clacEntityRanks(tsvScoreFile,minIncommings);
                indexingConfig.put(KEY_ENTITY_RANKINGS, entityRankings);
                log.info(String.format(" ... set min required score to %s (represents %s incomming links",minRequiredRanking,minIncommings));
                indexingConfig.put(KEY_REQUIRED_ENTITY_RANKING, minRequiredRanking);
            } else {
                log.error(String.format("Parsed File with the incommung links is invalid (esists:%s,isFile:%s,canRead:%s)",
                        tsvScoreFile.exists(), tsvScoreFile.isFile(), tsvScoreFile.canRead()));
            }
        } else {
            if(line.hasOption("ri")){
                log.warn("Option --requiredIncomming/-ri is only supported of Option --incomming/-i is active!");
            }
        }
        //THis mode uses the id of the entity rnking map as main lookup for
        //entities to index. This is faster than the usual mode if less than
        //50% of the entities are indexed!
        if(line.hasOption("r")){
            //resume makes only really sense with the RANKING BASED MODE
            indexingConfig.put(KEY_INDEXING_MODE, IndexingMode.RANKING_MAP_BASED);
            //set the RESUME MODE
            indexingConfig.put(KEY_RESUME_MODE, Boolean.TRUE);
        }
        RdfIndexer indexer = new RdfIndexer(indexingConfig);
        indexer.index();
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Float> clacEntityRanks(File tsvScoreFile,int minIncommings) throws IOException {
        TreeMap<String,Float> entityRankings = new TreeMap<String, Float>();
        final Iterator<String> lines = IOUtils.lineIterator(
                new FileInputStream(tsvScoreFile), "utf-8");
        long lineNumber = 0;
        int maxIncommung = 0;
        float maxScore = 0;
        long filtered  = 0;
        log.info("  ... init Entity Ranks based on "+tsvScoreFile);
        while (lines.hasNext()) {
            String line = lines.next();
            String[] parts = line.split("\t");
            if (parts.length != 2) {
                log.warn(String.format("skipping line: '%s'", line));
            }
            int incomming = Integer.parseInt(parts[1].trim());
            // take the log to avoid over popular entities to
            // dominate the results (attenuate the Zipf law of
            // culturally generated distribution)
            float score = (float)Math.log1p(incomming);
            if (lineNumber == 0 && score > 0) {
                maxIncommung = incomming;
                maxScore = score;
                if(minIncommings <= 0){
                    minRequiredRanking = -1f; //deactivate
                } else {
                    float min = (float)Math.log1p(minIncommings);
                    minRequiredRanking = min/maxScore;
                    if(minRequiredRanking > 1){
                        log.error("Parsed minimum required incomming links is bigger than the highest number of incomming links for any entity!");
                        System.exit(0);
                    }
                }
            }
            score = score/maxScore;
            if(score > 1){
                log.error("Found Entity wiht more incomming links than the entity in the first line");
                log.error("current:"+line);
                System.exit(0);
            }
            if(score >= minRequiredRanking){
                entityRankings.put(parts[0], score);
            } else {
                filtered ++;
            }
            lineNumber++;
        }
        log.info(String.format("  ... processed %s entities (%s with ranking > required | %s filtered",
                lineNumber,lineNumber-filtered,filtered));
        return entityRankings;
    }

}
