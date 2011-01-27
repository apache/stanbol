package org.apache.stanbol.autotagging.jena;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 * Read a Jena model and extract the most popular resources given by a
 * "tab separated values" files that holds the rank information of the
 * resources.
 *
 * For instance such as file can be computed from the page links info of DBpedia
 * using the corpusmaker toolkit: http://github.com/ogrisel/corpusmaker
 *
 * As it takes from 1h to 3h to compute such statistics, a precomputed file is
 * available here:
 *
 * http://dl.dropbox.com/u/5743203/IKS/autotagging/incoming-counts-redirected.
 * tsv.gz
 *
 * @author ogrisel
 */
public class ModelResampler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected int maxTopResources = 10000;

    protected File tsvScoreFile;

    public ModelResampler() {
        // use default values
    }

    public ModelResampler withMaxTopResources(int maxTopResources) {
        this.maxTopResources = maxTopResources;
        return this;
    }

    public ModelResampler withPrecomputedScoresFile(File tsvRanksFile) {
        tsvScoreFile = tsvRanksFile;
        return this;
    }

    /**
     * Perform a query that returns a result set iterating over all typed
     * resource. The ordering of the results is undefined.
     *
     * @param model the model to query
     * @return a result set where 'resource' is bound to the a resource
     */
    public ResultSet queryAllResources(Model model) {
        QuerySolution mapping = new QuerySolutionMap();
        StringBuilder qb = new StringBuilder();
        qb.append("SELECT distinct ?resource   ");
        qb.append("{ ");
        qb.append(" ?resource a ?type . ");
        qb.append(" FILTER ( isURI(?resource) ) . ");
        qb.append("} ");
        Query q = QueryFactory.create(qb.toString(), Syntax.syntaxARQ);
        QueryExecution qexec = QueryExecutionFactory.create(q, model, mapping);
        return qexec.execSelect();
    }

    /**
     * Perform a query to find the top popular resources by counting incoming
     * links. The score values are normalized (the most popular resource as a
     * score of 1.0, unless all scores are 0.0).
     *
     * @param model the model to query
     * @return a result set where 'resource' is bound to a popular resource
     */
    public Iterator<ResourceInfo> queryTopResources(Model model) {
        QuerySolution mapping = new QuerySolutionMap();
        StringBuilder qb = new StringBuilder();
        qb.append("SELECT ?resource ( count(?incoming) AS ?count )  ");
        qb.append("{ ");
        qb.append(" ?resource a ?type . ");
        qb.append(" OPTIONAL { ?incoming ?relationship ?resource . } . ");
        qb.append(" FILTER ( isURI(?resource) ) . ");
        qb.append("} ");
        qb.append("GROUP BY ?resource ");
        qb.append("ORDER BY DESC ( ?count ) ");
        qb.append(String.format("OFFSET 0 LIMIT %d", maxTopResources));
        Query q = QueryFactory.create(qb.toString(), Syntax.syntaxARQ);
        final ResultSet resultSet = QueryExecutionFactory.create(q, model,
                mapping).execSelect();
        return new Iterator<ResourceInfo>() {

            long sampled = 0;

            double maxScore = 1.0;

            public boolean hasNext() {
                return resultSet.hasNext();
            }

            public ResourceInfo next() {
                QuerySolution nextSolution = resultSet.nextSolution();
                double count = nextSolution.getLiteral("count").getDouble();
                double score = Math.log1p(count);
                if (sampled == 0 && count > 0) {
                    maxScore = score;
                }
                sampled++;
                return new ResourceInfo(nextSolution.getResource("resource"),
                        score / maxScore);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public Iterator<ResourceInfo> findTopResources(final Model model)
            throws FileNotFoundException, IOException {
        if (tsvScoreFile == null) {
            return queryTopResources(model);
        }
        final Iterator<String> lines = IOUtils.lineIterator(
                new FileInputStream(tsvScoreFile), "utf-8");
        return new Iterator<ResourceInfo>() {

            double maxScore = 1.0;

            int sampled = 0;

            ResourceInfo nextRi = null;

            protected ResourceInfo fetchNext(boolean andForget) {
                ResourceInfo result = nextRi;
                if (result == null) {
                    if (lines.hasNext()) {
                        String line = lines.next();
                        String[] parts = line.split("\t");
                        if (parts.length != 2) {
                            log.warn(String.format("skipping line: '%s'", line));
                            return fetchNext(andForget);
                        }
                        double score = Double.parseDouble(parts[1].trim());
                        // take the log to avoid over popular entities to
                        // dominate the results (attenuate the Zipf law of
                        // culturally generated distribution)
                        score = Math.log1p(score);
                        if (sampled == 0 && score > 0) {
                            maxScore = score;
                        }
                        String resource = parts[0].trim();
                        if (!resource.startsWith("http://")) {
                            resource = "http://dbpedia.org/resource/"
                                    + resource;
                        }
                        Resource r = model.createResource(resource);
                        if (!model.containsResource(r)) {
                            log.debug(String.format(
                                    "skipping resource: '%s', not found in model",
                                    resource));
                            return fetchNext(andForget);
                        }
                        result = new ResourceInfo(r, score / maxScore);
                    }
                }
                nextRi = andForget ? null : result;
                return result;
            }

            public boolean hasNext() {
                return sampled < maxTopResources && fetchNext(false) != null;
            }

            public ResourceInfo next() {
                ResourceInfo next = fetchNext(true);
                sampled++;
                return next;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Iteratively sample statements carried by popular resources of sourceModel
     * into targetModel.
     *
     * @param sourceModel model to sample popular resources from
     * @param targetModel model to save resource attributes to
     * @return an iterator over popular resource to monitor progress
     * @throws IOException
     * @throws FileNotFoundException
     */
    public Iterator<ResourceInfo> samplerIterator(final Model sourceModel,
            final Model targetModel) throws FileNotFoundException, IOException {
        final Iterator<ResourceInfo> topResources = findTopResources(sourceModel);
        return new Iterator<ResourceInfo>() {

            public boolean hasNext() {
                return topResources.hasNext();
            }

            public ResourceInfo next() {
                ResourceInfo ri = topResources.next();
                StmtIterator stmts = sourceModel.listStatements(ri.resource,
                        null, null, null);
                targetModel.add(stmts);
                targetModel.add(targetModel.createLiteralStatement(
                        ri.resource,
                        targetModel.getProperty(ModelIndexer.POPULARITY_SCORE_PROPERTY),
                        ri.score));
                return ri;
            }

            public void remove() {
                throw new NotImplementedException();
            }
        };
    }

    /**
     * Extract the most popular resources ranked by incoming relation into s *
     * targetModel.
     *
     * @param sourceModel model to extract popular resource from
     * @param targetModel model where to save the extracted resources data
     *
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void extractMostPopular(Model sourceModel, Model targetModel)
            throws FileNotFoundException, IOException {
        log.info("computing the list of resources to sample...");
        long lastTime = System.currentTimeMillis();
        Iterator<ResourceInfo> iterator = samplerIterator(sourceModel,
                targetModel);
        long newTime = System.currentTimeMillis();
        log.info(String.format("query took %fs", (newTime - lastTime) / 1000.));
        lastTime = newTime;
        int i = 1;
        long checkpointSize = 5000;
        while (iterator.hasNext()) {
            ResourceInfo ri = iterator.next();
            if (i == 1 && ri.score == 0.0f) {
                log.warn(String.format(
                        "most popular resource '%s' has a score of 0.0...",
                        ri.resource.getURI()));
            }
            if (i % checkpointSize == 0) {
                newTime = System.currentTimeMillis();
                double duration = (newTime - lastTime) / 1000.;
                String uri = ri.resource.getURI();
                try {
                    log.info(String.format(
                            "sampled resource %09d (at '%s' with score %f) - %f entities/s",
                            i, URLDecoder.decode(uri, "UTF-8"), ri.score,
                            checkpointSize / duration));
                } catch (UnsupportedEncodingException e) {
                    log.warn(String.format("invalid URI '%s': %s", uri,
                            e.getMessage()));
                }
                lastTime = newTime;
            }
            i++;
        }
        log.info(String.format("successfully sampled %09d resources", i));
    }

    public static void resample(File srcTdbFolder, File targetFile,
            File scoresFile, int maxTopResources) throws IOException {
        Model sourceModel = TDBFactory.createModel(srcTdbFolder.getAbsolutePath());

        String filename = targetFile.getName();
        String format = null;
        boolean useTemporaryModel = false;
        if (filename.endsWith(".nt")) {
            format = "N-TRIPLE";
            useTemporaryModel = true;
        } else if (filename.endsWith(".n3")) {
            format = "N3";
            useTemporaryModel = true;
        } else if (filename.endsWith(".xml")) {
            // format = null will use the XML syntax
            useTemporaryModel = true;
        }

        // TODO: use a temporary TDB model in a temporary directory instead of
        // a memory model that lacks scalability?
        Model targetModel = useTemporaryModel ? ModelFactory.createDefaultModel()
                : TDBFactory.createModel(targetFile.getAbsolutePath());

        ModelResampler sampler = new ModelResampler().withMaxTopResources(
                maxTopResources).withPrecomputedScoresFile(scoresFile);
        sampler.extractMostPopular(sourceModel, targetModel);

        if (useTemporaryModel) {
            targetModel.write(new FileOutputStream(targetFile), format, null);
        }
    }
}
