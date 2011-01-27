package org.apache.stanbol.autotagging.jena;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.stanbol.autotagging.Autotagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDBFactory;


/**
 * Build a Lucene index out of a Jena model.
 *
 * @author ogrisel
 */
public class ModelIndexer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(ModelIndexer.class);

    public static final String URI_FIELD = "uri";

    public static final String DEFAULT_DBPEDIA_SAMPLE = "dbpedia/dbpedia-sample-10000.nt";

    public static final String POPULARITY_SCORE_PROPERTY = "http://www.iksproject.eu/ns/popularity-score";

    private final IndexWriter iwriter;

    private final Model model;

    // reduce GC load by reusing Document and Fields instances
    private final Map<String, Field> literalFields = new HashMap<String, Field>();

    private final Map<String, Map<String, Field>> uriFields = new HashMap<String, Map<String, Field>>();

    private final Document doc = new Document();

    private final Map<String, Float> boostedFields = new HashMap<String, Float>();

    private final String scorePropertyUri = POPULARITY_SCORE_PROPERTY;

    private Field getField(String property, String value, boolean isLiteral) {
        // make the cache key
        if (isLiteral) {
            Field cachedField = literalFields.get(property);
            if (cachedField == null) {
                cachedField = new Field(property, value, Field.Store.NO,
                        Field.Index.ANALYZED);
                literalFields.put(property, cachedField);
            } else {
                cachedField.setValue(value);
            }
            return cachedField;
        } else {
            // TODO: make sure that the multivalued URI properties take value in
            // a limit size controlled vocabulary which is the case for types,
            // but not for relations between entities
            Map<String, Field> cachedFields = uriFields.get(property);
            if (cachedFields == null) {
                cachedFields = new HashMap<String, Field>();
                uriFields.put(property, cachedFields);
            }
            Field cachedField = cachedFields.get(value);
            if (cachedField == null) {
                cachedField = new Field(property, value, Field.Store.YES,
                        Field.Index.NOT_ANALYZED);
                cachedFields.put(value, cachedField);
            }
            return cachedField;
        }
    }

    public ModelIndexer(final IndexWriter iwriter, final Model model) {
        this.iwriter = iwriter;
        this.model = model;

        // by default boost the title (a.k.a. rdfs:label of the entity)
        boostedFields.put("http://www.w3.org/2000/01/rdf-schema#label", 3.0f);
    }

    public Map<String, Float> getBoostedFields() {
        return boostedFields;
    }

    public void close() throws IOException {
        iwriter.close();
        model.close();
    }

    public Iterator<Document> indexIterator() {
        ModelResampler sampler = new ModelResampler();
        final ResultSet resultSet = sampler.queryAllResources(model);
        final Property scoreProperty = model.getProperty(scorePropertyUri);

        return new Iterator<Document>() {

            public boolean hasNext() {
                return resultSet.hasNext();
            }

            public Document next() {
                QuerySolution solution = resultSet.next();
                Resource r = solution.getResource("resource");
                StmtIterator stmts = model.listStatements(r, null, null, null);
                doc.getFields().clear();
                doc.add(getField(URI_FIELD, r.getURI(), false));
                List<Statement> stmtList = stmts.toList();

                // find document boost info if any
                float docBoost = 1.0f;
                Statement toDelete = null;
                for (Statement stmt : stmtList) {
                    if (stmt.getPredicate().equals(scoreProperty)) {
                        docBoost = stmt.getFloat();
                        toDelete = stmt;
                    }
                }
                if (toDelete != null) {
                    stmtList.remove(toDelete);
                }

                // index all statement objects as lucene fields
                for (Statement stmt : stmtList) {
                    String text;
                    boolean isLiteral = stmt.getObject().isLiteral();
                    if (isLiteral) {
                        text = stmt.getObject().as(Literal.class).getString();
                    } else if (stmt.getObject().isURIResource()) {
                        text = stmt.getObject().as(Resource.class).getURI();
                    } else {
                        // skip non indexable nodes (blank nodes, seqs, bags,
                        // ...)
                        continue;
                    }
                    Field field = getField(stmt.getPredicate().toString(),
                            text, isLiteral);
                    Float boost = boostedFields.get(field.name());
                    if (boost != null) {
                        field.setBoost(boost * docBoost);
                    } else {
                        field.setBoost(docBoost);
                    }
                    doc.add(field);
                }
                try {
                    iwriter.addDocument(doc);
                } catch (Exception e) {
                    log.error("error indexing " + r.getURI(), e);
                    return doc;
                }
                return doc;

            }

            public void remove() {
                throw new NotImplementedException();
            }
        };
    }

    public static void index(Model model, IndexWriter writer, boolean close)
            throws IOException {
        ModelIndexer indexer = new ModelIndexer(writer, model);
        try {
            log.info("computing the list of entities to process...");
            long lastTime = System.currentTimeMillis();
            Iterator<Document> iterator = indexer.indexIterator();
            long newTime = System.currentTimeMillis();
            log.info(String.format("query took %fs",
                    (newTime - lastTime) / 1000.));
            lastTime = newTime;
            int i = 1;
            long checkpointSize = 5000;
            while (iterator.hasNext()) {
                Document doc = iterator.next();
                if (i % checkpointSize == 0) {
                    writer.commit();
                    newTime = System.currentTimeMillis();
                    double duration = (newTime - lastTime) / 1000.;
                    log.info(String.format(
                            "indexed entity %09d '%s' at %f entities/s", i,
                            URLDecoder.decode(doc.get(URI_FIELD), "UTF-8"),
                            checkpointSize / duration));
                    lastTime = newTime;
                }
                i++;
            }
            writer.commit();
            log.info(String.format(
                    "successfully indexed %09d entities, now optimizing the index",
                    i));
            writer.optimize();
        } finally {
            if (close) {
                indexer.close();
            }
        }
    }

    public static void index(File tdbModel, File fsDirectory)
            throws CorruptIndexException, LockObtainFailedException,
            IOException {
        Model model = TDBFactory.createModel(tdbModel.getAbsolutePath());
        index(model, fsDirectory);
    }

    public static void index(Model model, File fsDirectory)
            throws CorruptIndexException, LockObtainFailedException,
            IOException {
        MaxFieldLength maxFieldLength = new MaxFieldLength(100000);
        // TODO: re-enable shingles once we can get rid of the "-"
        // shingles
        IndexWriter writer = new IndexWriter(FSDirectory.open(fsDirectory),
                Autotagger.getDefaultAnalyzer(), true, maxFieldLength);
        writer.setRAMBufferSizeMB(42);
        index(model, writer, true);
    }

    public static String DEFAULT_INDEX_DIRECTORY() {
        return "default-iks-autotagging-idx";
    }

    public static File buildDefaultIndex() throws CorruptIndexException,
            LockObtainFailedException, IOException {
        return buildDefaultIndex(null, false);
    }

    public static File buildDefaultIndex(File folder, boolean deleteExisting)
            throws CorruptIndexException, LockObtainFailedException,
            IOException {
        if (folder == null) {
            folder = new File(System.getProperty("java.io.tmpdir"));
        }
        File fsDirectory = new File(folder, DEFAULT_INDEX_DIRECTORY());
        if (deleteExisting) {
            log.info("deleting default indexed model in: "
                    + fsDirectory.getAbsolutePath());
            FileUtils.deleteDirectory(fsDirectory);
        }
        if (!fsDirectory.exists()) {
            log.info("creating default indexed model in: "
                    + fsDirectory.getAbsolutePath());
            InputStream stream = ModelIndexer.class.getClassLoader().getResourceAsStream(
                    DEFAULT_DBPEDIA_SAMPLE);
            if (stream == null) {
                throw new IOException("could not find resource: "
                        + DEFAULT_DBPEDIA_SAMPLE);
            }
            Model model = ModelFactory.createDefaultModel();
            model.read(stream, null, "N-TRIPLE");
            index(model, fsDirectory);
        }
        return fsDirectory;
    }

}
