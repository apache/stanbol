package eu.iksproject.fise.store;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.fise.servicesapi.ContentItem;
import eu.iksproject.fise.servicesapi.Store;
import eu.iksproject.fise.store.jcr.JCRContentItem;

/**
 * JCR Store for standalone FISE server
 *
 * @scr.property name="service.ranking" type=Integer value=10000
 */
@Component(immediate = false)
@Service
public class JCRStore implements Store {

    private static final Logger log = LoggerFactory.getLogger("eu.iksproject.fise.store.JCRStore");

    private static final String FISE_ROOT_NODE = "fise";

    private final Map<String, ContentItem> data = new HashMap<String, ContentItem>();

    @Reference
    private WeightedTcProvider tcProvider;

    @Reference
    private Repository repository;

    private static Session session = null;

    public ContentItem create(String id, byte[] content, String mimeType) {
        final MGraph g = tcProvider.createMGraph(new UriRef(id));

        ContentItem node;
        try {
            node = new JCRContentItem(id, content, mimeType, g, getParentNode());
            return node;
        } catch (ItemExistsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (VersionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConstraintViolationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LockException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public ContentItem get(String id) {
        try {
            if (findNodeById(id, getParentNode()) != null) {
                return new JCRContentItem(id, getParentNode());
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public String put(ContentItem ci) {
        byte[] bytes = new byte[0];// ci.getStream();
        ContentItem result;
        try {
            result = new JCRContentItem(ci.getId(), bytes, ci.getMimeType(), ci
                    .getMetadata(), getParentNode());
            return result.getId();
        } catch (ItemExistsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (VersionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConstraintViolationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LockException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static Node findNodeById(String id, Node parent)
            throws RepositoryException, InvalidQueryException {
        log.info("entering findByNode with id " + id);
        if (id != null && id.length() > 0) {
            QueryManager queryManager = parent.getSession().getWorkspace()
                    .getQueryManager();
            String queryString = "/jcr:root/" + parent.getPath() + "//*[@"
                    + JCRContentItem.FISE_ID_PROP + "= '" + id + "']";
            Query query = queryManager.createQuery(queryString, "xpath");
            QueryResult results = query.execute();
            NodeIterator r = results.getNodes();
            while (r.hasNext()) {
                log.info("found node for id " + id);
                return r.nextNode();
            }
        }
        log.info("found NO node for id " + id);
        return null;
    }

    public String toString() {
        return getClass().getName();
    }

    private Node getParentNode() throws RepositoryException {
        Session session = getSession();
        Node rootNode = session.getRootNode();
        if (rootNode.hasNode(FISE_ROOT_NODE)) {
            return rootNode.getNode(FISE_ROOT_NODE);
        } else {
            return rootNode.addNode(FISE_ROOT_NODE);
        }
    }

    private Session getSession() {
        if (session != null) {
            return session;
        }
        try {
            return repository.login(new SimpleCredentials("admin", "admin"
                    .toCharArray()));
        } catch (LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
