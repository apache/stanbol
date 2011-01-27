package org.apache.stanbol.enhancer.engines.metaxa.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.semanticdesktop.aperture.extractor.Extractor;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.extractor.ExtractorFactory;
import org.semanticdesktop.aperture.extractor.ExtractorRegistry;
import org.semanticdesktop.aperture.extractor.impl.DefaultExtractorRegistry;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.RDFContainerFactory;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerFactoryImpl;
import org.semanticdesktop.aperture.vocabulary.NIE;

/**
 * {@link MetaxaCore} provides the functionality to extract metadata and text
 * for a number of different document formats (pdf, html, etc.).
 *
 * @author Joerg Steffen, DFKI
 * @version $Id$
 */
public class MetaxaCore {

    /**
     * This contains the configured extractors.
     */
    private ExtractorRegistry extractorRegistry;

    /**
     * This creates a new instance of {@code ApertureExtractor} using the given
     * configuration.
     *
     * @param configFileName
     *            a {@link String} with the config file name.
     * @throws IOException
     *             if there is an error during initialization
     */
    public MetaxaCore(String configFileName)
            throws IOException {

        InputStream in =
            getClass().getClassLoader().getResourceAsStream(configFileName);
        this.extractorRegistry = new DefaultExtractorRegistry(in);
    }

    /**
     * This return {@code true} if the given MIME type is supported by the
     * extractor.
     *
     * @param mimeType
     *            a {@link String} with the MIME type
     * @return a {@code boolean}
     */
    public boolean isSupported(String mimeType) {
        @SuppressWarnings("rawtypes")
        Set factories = this.extractorRegistry.getExtractorFactories(mimeType);
        return factories != null && !factories.isEmpty();
    }


    /**
     * This returns a model containing all the metadata that could be extracted
     * by reading the given input stream using the given MIME type.
     *
     * @param in
     *            an {@link InputStream} where to read the document from
     * @param docId
     *            a {@link String} with the document URI
     * @param mimeType
     *            a {@link String} with the MIME type
     * @return a {@link Model} containing the metadata or {@code null} if no
     *         extractor is available for the given MIME type
     * @throws ExtractorException
     *             if there is an error when extracting the metadata
     * @throws IOException
     *             if there is an error when reading the input stream
     */
    public Model extract(
            InputStream in, String docId, String mimeType)
            throws ExtractorException, IOException {

        @SuppressWarnings("rawtypes")
        Set factories = this.extractorRegistry.getExtractorFactories(mimeType);
        Model result = null;
        if (factories != null && !factories.isEmpty()) {
            // get extractor from the first available factory
            ExtractorFactory factory =
                (ExtractorFactory)factories.iterator().next();
            Extractor extractor = factory.get();
            RDFContainerFactory containerFactory =
                new RDFContainerFactoryImpl();
            RDFContainer container =
                containerFactory.getRDFContainer(new URIImpl(docId));
            extractor.extract(
                container.getDescribedUri(),
                new BufferedInputStream(in, 8192),
                null, mimeType, container);
            in.close();
            result = container.getModel();
        }

        return result;
    }

    /**
     * This returns a documents plain text if contained in the given extracted
     * metadata.
     *
     * @param model
     *            a {@link Model} with the extracted metadata
     * @return a {@link String} with the plain text content or {@code null} if
     *         no plain text was contained in the extracted metadata
     */
    public static String getText(Model model) {
        String result = null;
        ClosableIterator<Statement> statements = null;
        try {
            statements =
                model.findStatements(
                Variable.ANY, NIE.plainTextContent, Variable.ANY);
            StringBuilder text = new StringBuilder(10000);
            while (statements.hasNext()) {
                Statement statement = statements.next();
                Node value = statement.getObject();
                if (value instanceof Literal) {
                    text.append(((Literal)value).getValue());
                }
            }
            result = text.toString().trim();
            if (result.length() == 0) {
                result = null;
            }
        } finally {
            if (statements != null) {
                statements.close();
            }
        }
        return result;
    }

}
