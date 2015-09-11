package org.apache.stanbol.enhancer.engines.entitycoreference;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDFS_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.stanbol.entityhub.core.model.EntityImpl;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;

/**
 * 
 * @author Cristian Petroaca
 *
 */
public class MockEntityCorefDbpediaSite implements Site {
	public static final String SITE_ID = "entity-coref-dbpedia";

	private Map<String, Entity> entities;

	public MockEntityCorefDbpediaSite() {
		entities = new HashMap<>();
		InMemoryValueFactory valueFactory = InMemoryValueFactory.getInstance();

		Representation merkelRep = valueFactory.createRepresentation("http://dbpedia.org/page/Angela_Merkel");
		merkelRep.set("http://dbpedia.org/ontology/country", "http://dbpedia.org/resource/Germany");
		merkelRep.set(RDF_TYPE.getUnicodeString(), "http://dbpedia.org/class/yago/Politician110451263");
		merkelRep.set(RDFS_LABEL.getUnicodeString(), valueFactory.createText("Angela Merkel", "en"));

		entities.put("http://dbpedia.org/page/Angela_Merkel", new EntityImpl(SITE_ID, merkelRep, null));

		Representation politicianRep = valueFactory
				.createRepresentation("http://dbpedia.org/class/yago/Politician110451263");
		politicianRep.set(RDFS_LABEL.getUnicodeString(), valueFactory.createText("politician", "en"));
		entities.put("http://dbpedia.org/class/yago/Politician110451263", new EntityImpl(SITE_ID, politicianRep, null));

	}

	@Override
	public String getId() {
		return null;
	}

	@Override
	public QueryResultList<String> findReferences(FieldQuery query) throws SiteException {
		return null;
	}

	@Override
	public QueryResultList<Representation> find(FieldQuery query) throws SiteException {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public QueryResultList<Entity> findEntities(FieldQuery query) throws SiteException {
		TextConstraint labelConstraint = (TextConstraint) query.getConstraint(RDFS_LABEL.getUnicodeString());

		for (Entity entity : entities.values()) {
			Iterator<Object> entityAttributes = entity.getRepresentation().get(RDFS_LABEL.getUnicodeString());

			while (entityAttributes.hasNext()) {
				Text entityAttribute = (Text) entityAttributes.next();

				if (entityAttribute.getText().equals(labelConstraint.getText())) {
					Collection<Entity> retEntities = new ArrayList<>(1);
					retEntities.add(entity);
					return new QueryResultListImpl<Entity>(null, retEntities, Entity.class);
				}
			}
		}

		return null;
	}

	@Override
	public Entity getEntity(String id) throws SiteException {
		return entities.get(id);
	}

	@Override
	public InputStream getContent(String id, String contentType) throws SiteException {
		return null;
	}

	@Override
	public FieldMapper getFieldMapper() {
		return null;
	}

	@Override
	public FieldQueryFactory getQueryFactory() {
		return DefaultQueryFactory.getInstance();
	}

	@Override
	public SiteConfiguration getConfiguration() {
		return null;
	}

	@Override
	public boolean supportsLocalMode() {
		return false;
	}

	@Override
	public boolean supportsSearch() {
		return false;
	}

}
