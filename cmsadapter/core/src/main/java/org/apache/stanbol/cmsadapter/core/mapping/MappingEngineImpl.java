package org.apache.stanbol.cmsadapter.core.mapping;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.core.decorated.DObjectFactoryImp;
import org.apache.stanbol.cmsadapter.jcr.processor.ConceptBridgesProcesser;
import org.apache.stanbol.cmsadapter.jcr.processor.InstanceBridgesProcesser;
import org.apache.stanbol.cmsadapter.jcr.processor.JCRNodeTypeLifter;
import org.apache.stanbol.cmsadapter.jcr.processor.SubsumptionBridgesProcesser;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.NamingStrategy;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessManager;
import org.apache.stanbol.ontologymanager.store.rest.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;

@Component(factory = "org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngineFactory")
@Service
public class MappingEngineImpl implements MappingEngine {
    private static final Logger logger = LoggerFactory.getLogger(MappingEngineImpl.class);

    /*
     * public static final String PROPERTY_ONTOLOGY_URI = "ontology.uri"; public static final String
     * PROPERTY_CONNECTION_INFO = "connection.info";
     * 
     * @Property(name = PROPERTY_ONTOLOGY_URI) private String ontologyURI;
     * 
     * @Property(name = PROPERTY_CONNECTION_INFO) private ConnectionInfo connectionInfo;
     */
    @Reference
    private RestClient storeClient;
    @Reference
    private RepositoryAccessManager accessManager;

    private Object session;
    private OntModel ontModel;
    private String ontologyURI;
    private BridgeDefinitions bridgeDefinitions;
    private OntologyResourceHelper ontologyResourceHelper;
    private DObjectAdapter adapter;
    private NamingStrategy namingStrategy;

    public MappingEngineImpl() {

    }

    @Activate
    public void activate(final Map<?,?> properties) {

    }

    public Object getSession() {
        return session;
    }

    public OntModel getOntModel() {
        return ontModel;
    }

    public BridgeDefinitions getBridgeDefinitions() {
        return bridgeDefinitions;
    }

    public OntologyResourceHelper getOntologyResourceHelper() {
        return ontologyResourceHelper;
    }

    public String getOntologyURI() {
        return ontologyURI;
    }

    @Override
    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    @Override
    public void mapCR(OntModel model, String ontologyURI, List<CMSObject> cmsObjects) throws RepositoryAccessException {
        ConnectionInfo connectionInfo = OntologyResourceHelper.getConnectionInfo(model);
        RepositoryAccess accessor = accessManager.getRepositoryAccessor(connectionInfo);
        this.session = accessor.getSession(connectionInfo);
        this.bridgeDefinitions = OntologyResourceHelper.getBridgeDefinitions(model);
        this.ontologyURI = ontologyURI;
        this.ontModel = model;
        this.namingStrategy = new DefaultNamingStrategy(accessor, session, ontModel);
        this.ontologyResourceHelper = new OntologyResourceHelper(this);
        this.adapter = new DObjectFactoryImp(accessor, session);

        long t1 = System.currentTimeMillis();
        new ConceptBridgesProcesser(this).processUpdates(cmsObjects);

        new SubsumptionBridgesProcesser(this).processUpdates(cmsObjects);

        new InstanceBridgesProcesser(this).processUpdates(cmsObjects);
        logger.debug("Total process time for ontology {} is {} ms", ontologyURI, System.currentTimeMillis()
                                                                                 - t1);

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            RDFWriter rdfWriter = ontModel.getWriter("RDF/XML");
            rdfWriter.setProperty("xmlbase", ontologyURI);
            rdfWriter.write(ontModel, bos, ontologyURI);
            byte[] ontologyContentAsByteArray = bos.toByteArray();
            String ontologyContentAsString = new String(ontologyContentAsByteArray);
            storeClient.saveOntology(ontologyContentAsString, ontologyURI, "UTF-8");

        } catch (Exception ex) {
            logger.error("Exception occurred while saving the ontology");
            logger.error(ex.getMessage());
            // TODO return error message to flex side
        }
    }

    @Override
    public void mapCR(BridgeDefinitions bridges, ConnectionInfo connectionInfo, String ontologyURI) throws RepositoryAccessException {

        RepositoryAccess accessor = accessManager.getRepositoryAccessor(connectionInfo);
        this.session = accessor.getSession(connectionInfo);
        this.bridgeDefinitions = bridges;
        this.ontologyURI = ontologyURI;
        this.ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        this.namingStrategy = new DefaultNamingStrategy(accessor, session, ontModel);
        this.ontologyResourceHelper = new OntologyResourceHelper(this);
        this.adapter = new DObjectFactoryImp(accessor, session);

        long t1 = System.currentTimeMillis();
        try {
            new JCRNodeTypeLifter(this).lift();
        } catch (RepositoryException e) {
            logger.error("Lifting error", e);
        }

        // TODO Currently there are only JCR Processors
        new ConceptBridgesProcesser(this).processBridges();

        // Processing SubsumptionBridges
        new SubsumptionBridgesProcesser(this).processBridges();

        // Processing InstanceBridges
        new InstanceBridgesProcesser(this).processBridges();

        // save connection info
        OntologyResourceHelper.saveConnectionInfo(connectionInfo, ontModel);
        // save bridge definitions
        // deserialize
        OntologyResourceHelper.saveBridgeDefinitions(bridgeDefinitions, ontModel);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        logger.debug("Total process time for ontology {} is {} ms", ontologyURI, System.currentTimeMillis()
                                                                                 - t1);

        try {
            RDFWriter rdfWriter = ontModel.getWriter("RDF/XML");
            rdfWriter.setProperty("xmlbase", ontologyURI);
            rdfWriter.write(ontModel, bos, ontologyURI);
            byte[] ontologyContentAsByteArray = bos.toByteArray();
            String ontologyContentAsString = new String(ontologyContentAsByteArray);
            storeClient.saveOntology(ontologyContentAsString, ontologyURI, "UTF-8");

        } catch (Exception ex) {
            logger.error("Exception occurred while saving the ontology");
            logger.error(ex.getMessage());
            // TODO return error message to flex side
        }

    }

    @Override
    public void liftNodeTypes(ConnectionInfo connectionInfo, String ontologyURI) {
        // TODO Auto-generated method stub

    }

    @Override
    public RepositoryAccessManager getRepositoryAccessManager() {
        return accessManager;
    }

    @Override
    public DObjectAdapter getDObjectAdapter() {
        return adapter;
    }
}
