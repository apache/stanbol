package org.apache.stanbol.autotagging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.stanbol.autotagging.jena.ModelIndexer;
import org.apache.stanbol.autotagging.jena.ModelResampler;
import org.apache.stanbol.autotagging.jena.ResourceInfo;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;


public class ModelResamplerTest {

    protected Model srcModel;

    protected Model targetModel;

    protected Resource[] r;

    protected Property p;

    protected Property score;

    protected Resource personClass;

    protected Property type;

    @Before
    public void makeModels() {
        srcModel = ModelFactory.createDefaultModel();
        targetModel = ModelFactory.createDefaultModel();

        // create properties and resources
        p = srcModel.createProperty("urn:p");
        score = srcModel.createProperty(ModelIndexer.POPULARITY_SCORE_PROPERTY);
        type = srcModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        personClass = srcModel.createResource("http://dbpedia.org/ontology/Person");
        r = new Resource[10];

        for (int i = 0; i < r.length; i++) {
            r[i] = srcModel.createResource(String.format("urn:%d", i));
            srcModel.add(r[i], type, personClass);
        }

        // connect resources with p
        srcModel.add(r[0], p, r[1]);
        srcModel.add(r[2], p, r[1]);
        srcModel.add(r[3], p, r[1]);
        srcModel.add(r[9], p, r[1]);

        srcModel.add(r[0], p, r[2]);
        srcModel.add(r[4], p, r[2]);
        srcModel.add(r[9], p, r[2]);

        srcModel.add(r[1], p, r[5]);
        srcModel.add(r[4], p, r[5]);

        srcModel.add(r[8], p, r[4]);

        srcModel.add(r[4], p, r[8]);

    }

    @Test
    public void testResampling() throws FileNotFoundException, IOException {
        ModelResampler sampler = new ModelResampler().withMaxTopResources(2);
        Iterator<ResourceInfo> samplerIterator = sampler.samplerIterator(
                srcModel, targetModel);

        assertTrue(samplerIterator.hasNext());
        ResourceInfo ri = samplerIterator.next();
        assertEquals(r[1], ri.resource);
        double r1Score = ri.score.doubleValue();
        assertEquals(1.0, r1Score, 0.01);

        assertTrue(samplerIterator.hasNext());
        ri = samplerIterator.next();
        assertEquals(r[2], ri.resource);
        double r2Score = ri.score.doubleValue();
        assertEquals(0.86, r2Score, 0.01);

        assertFalse(samplerIterator.hasNext());

        assertEquals(6, targetModel.size());

        assertTrue(targetModel.contains(r[1], type, personClass));
        assertTrue(targetModel.contains(r[1], p, r[5]));
        assertTrue(targetModel.containsLiteral(r[1], score, r1Score));

        assertTrue(targetModel.contains(r[2], type, personClass));
        assertTrue(targetModel.contains(r[2], p, r[1]));
        assertTrue(targetModel.containsLiteral(r[2], score, r2Score));
    }
}
