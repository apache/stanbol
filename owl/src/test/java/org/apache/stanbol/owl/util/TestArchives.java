package org.apache.stanbol.owl.util;

import java.io.File;
import java.net.URI;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class TestArchives {

    @Test
    public void testIRIMapper() throws Exception {
        URI uri = getClass().getResource("/ontologies/ontoarchive.zip").toURI();
        File f = new File(uri);
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        AutoIRIMapper mapp = new AutoIRIMapper(f, true);
        mgr.addIRIMapper(mapp);
        mapp.update();
    }

}
