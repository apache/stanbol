/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.stanbol.contenthub.store.solr;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.servicesapi.Constants;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDPathException;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.contenthub.store.solr.manager.SolrCoreManager;
import org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer;
import org.apache.stanbol.contenthub.store.solr.util.QueryGenerator;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.model.fields.FieldMapping;
import at.newmedialab.ldpath.model.programs.Program;

/**
 * 
 * @author anil.sinaci
 * 
 */
@Component(immediate = true)
@Service
public class SolrStoreImpl implements SolrStore {

	private static final Logger log = LoggerFactory
			.getLogger(SolrStoreImpl.class);

	@Reference
	private ManagedSolrServer managedSolrServer;

	@Reference
	private TcManager tcManager;

	@Reference
	private EnhancementJobManager jobManager;

	@Reference
	private SemanticIndexManager semanticIndexManager;

	private BundleContext bundleContext;

	@Activate
	protected void activate(ComponentContext context)
			throws IllegalArgumentException, IOException,
			InvalidSyntaxException, StoreException {
		if (managedSolrServer == null) {
			throw new IllegalStateException(
					"ManagedSolrServer cannot be referenced within SolrServerImpl.");
		}
		this.bundleContext = context.getBundleContext();
		SolrCoreManager.getInstance(bundleContext, managedSolrServer)
				.createDefaultSolrServer();
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		managedSolrServer = null;
	}

	@Override
	public SolrContentItem create(String id, byte[] content, String contentType) {
		return create(content, id, "", contentType, null);
	}

	@Override
	public MGraph getEnhancementGraph() {
		final UriRef graphUri = new UriRef(Constants.ENHANCEMENTS_GRAPH_URI);
		MGraph enhancementGraph = null;
		try {
			enhancementGraph = tcManager.getMGraph(graphUri);
		} catch (NoSuchEntityException e) {
			log.debug("Creating the enhancement graph!");
			enhancementGraph = tcManager.createMGraph(graphUri);
		}
		return enhancementGraph;
	}

	@Override
	public SolrContentItem create(byte[] content, String id, String title,
			String contentType, Map<String, List<Object>> constraints) {
		UriRef uri;
		if (id == null || id.isEmpty()) {
			uri = ContentItemHelper.makeDefaultUri(
					ContentItemIDOrganizer.CONTENT_ITEM_URI_PREFIX, content);
		} else {
			uri = new UriRef(ContentItemIDOrganizer.attachBaseURI(id));
		}
		log.debug("Created ContentItem with id:{} and uri:{}", id, uri);
		final MGraph g = new SimpleMGraph();
		return new SolrContentItemImpl(uri.getUnicodeString(), title, content,
				contentType, g, constraints);
	}

	private Object inferObjectType(Object val) {
		Object ret = null;
		try {
			ret = DateFormat.getInstance().parse(val.toString());
		} catch (Exception e) {
			try {
				ret = Long.valueOf(val.toString());
			} catch (Exception e1) {
				try {
					ret = Double.valueOf(val.toString());
				} catch (Exception e2) {
					try {
						ret = String.valueOf(val.toString());
					} catch (Exception e3) {
					}
				}
			}
		}

		if (ret == null)
			ret = val;
		return ret;
	}

	private String addSolrDynamicFieldExtension(String fieldName,
			Object[] values) {
		for (int i = 0; i < values.length; i++) {
			values[i] = inferObjectType(values[i]);
		}
		Object typed = values[0];
		String dynamicFieldName = fieldName;
		if (typed instanceof String) {
			dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_TEXT;
		} else if (typed instanceof Long) {
			dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_LONG;
		} else if (typed instanceof Double) {
			dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_DOUBLE;
		} else if (typed instanceof Date) {
			dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_DATE;
		}
		return dynamicFieldName;
	}

	private void enhance(SolrContentItem sci) throws StoreException {
		try {
			jobManager.enhanceContent(sci);
		} catch (EnhancementException e) {
			String msg = String.format("Cannot enhance content with id: {}",
					sci.getUri().getUnicodeString());
			log.error(msg, e);
			throw new StoreException(msg, e);
		}
		updateEnhancementGraph(sci);
	}

	private void removeEnhancements(String id) {
		MGraph enhancementGraph = getEnhancementGraph();
		Iterator<Triple> it = enhancementGraph.filter(new UriRef(id), null,
				null);
		List<Triple> willBeRemoved = new ArrayList<Triple>();
		while (it.hasNext()) {
			willBeRemoved.add(it.next());
		}
		enhancementGraph.removeAll(willBeRemoved);
	}

	private void updateEnhancementGraph(SolrContentItem sci) {
		MGraph enhancementGraph = getEnhancementGraph();
		// Delete old enhancements which belong to this content item from the
		// global enhancements graph.
		removeEnhancements(sci.getUri().getUnicodeString());
		// Add new enhancements of this content item to the global enhancements
		// graph.
		Iterator<Triple> it = sci.getMetadata().iterator();
		while (it.hasNext()) {
			Triple triple = null;
			try {
				triple = it.next();
				enhancementGraph.add(triple);
			} catch (Exception e) {
				log.warn(
						"Cannot add triple {} to the TCManager.enhancementgraph",
						triple, e);
				continue;
			}
		}
	}

	@Override
	public String enhanceAndPut(SolrContentItem sci) throws StoreException {
		enhance(sci);
		return put(sci);
	}

	@Override
	public String enhanceAndPut(SolrContentItem sci, String ldProgramName)
			throws StoreException {
		enhance(sci);
		return put(sci, ldProgramName);
	}

	@Override
	public String put(ContentItem ci) throws StoreException {
		SolrInputDocument doc = new SolrInputDocument();
		addDefaultFields(ci, doc);
		if (ci instanceof SolrContentItem) {
			SolrContentItem sci = (SolrContentItem) ci;
			addSolrSpecificFields(sci, doc);
		}
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext,
				managedSolrServer).getServer();
		try {
			solrServer.add(doc);
			solrServer.commit();
			log.debug("Documents are committed to Solr Server successfully.");
		} catch (SolrServerException e) {
			log.error("Solr Server Exception", e);
		} catch (IOException e) {
			log.error("IOException", e);
		}
		return ci.getUri().getUnicodeString();
	}

	@Override
	public String put(SolrContentItem sci, String ldProgramName)
			throws StoreException {
		if (ldProgramName == null
				|| ldProgramName.isEmpty()
				|| ldProgramName
						.equals(SolrCoreManager.CONTENTHUB_DEFAULT_INDEX_NAME)) {
			return put(sci);
		}
		SolrInputDocument doc = new SolrInputDocument();
		addDefaultFields(sci, doc);
		addSolrSpecificFields(sci, doc, ldProgramName);
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext,
				managedSolrServer).getServer(ldProgramName);
		try {
			solrServer.add(doc);
			solrServer.commit();
			log.debug("Documents are committed to Solr Server successfully.");
		} catch (SolrServerException e) {
			log.error("Solr Server Exception", e);
			throw new StoreException(e.getMessage(), e);
		} catch (IOException e) {
			log.error("IOException", e);
			throw new StoreException(e.getMessage(), e);
		}
		return sci.getUri().getUnicodeString();
	}

	private void addDefaultFields(ContentItem ci, SolrInputDocument doc)
			throws StoreException {
		if (ci.getUri().getUnicodeString() == null
				|| ci.getUri().getUnicodeString().isEmpty()) {
			log.debug("ID of the content item cannot be null while inserting to the SolrStore.");
			throw new IllegalArgumentException(
					"ID of the content item cannot be null while inserting to the SolrStore.");
		}

		String content = null;
		try {
			Entry<UriRef, Blob> contentPart = ContentItemHelper.getBlob(ci,
					Collections.singleton("text/plain"));
			if (contentPart == null) {
				throw new StoreException(
						"There is no textual for the content item");
			}
			content = ContentItemHelper.getText(contentPart.getValue());
		} catch (IOException ex) {
			String msg = "Cannot read the stream of the ContentItem.";
			log.error(msg, ex);
			throw new StoreException(msg, ex);
		}

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String creationDate = sdf.format(cal.getTime());

		doc.addField(SolrFieldName.ID.toString(), ci.getUri()
				.getUnicodeString());
		doc.addField(SolrFieldName.CONTENT.toString(), content);
		try {
			doc.addField(SolrFieldName.BINARYCONTENT.toString(),
					IOUtils.toByteArray(ci.getStream()));
		} catch (IOException e) {
			throw new StoreException(
					"Failed to get bytes of conten item stream", e);
		}
		doc.addField(SolrFieldName.MIMETYPE.toString(), ci.getMimeType());
		doc.addField(SolrFieldName.CREATIONDATE.toString(), creationDate);

		// add the number of enhancemets to the content item
		long enhancementCount = 0;
		Iterator<Triple> it = ci.getMetadata().filter(null,
				Properties.ENHANCER_EXTRACTED_FROM,
				new UriRef(ci.getUri().getUnicodeString()));
		while (it.hasNext()) {
			it.next();
			enhancementCount++;
		}
		doc.addField(SolrFieldName.ENHANCEMENTCOUNT.toString(),
				enhancementCount);
	}

	private boolean fieldAlreadyIndexedInProgram(String programName,
			String fieldName) {
		Program<Object> program = semanticIndexManager
				.getParsedProgramByName(programName);
		Iterator<FieldMapping<?, Object>> it = program.getFields().iterator();
		while (it.hasNext()) {
			FieldMapping<?, Object> fm = it.next();
			if (fm.getFieldName().equals(fieldName))
				return true;
		}
		return false;
	}

	private void addConstraints(SolrContentItem sci, SolrInputDocument doc,
			String programName) {
		if (sci.getConstraints() != null) {
			for (Entry<String, List<Object>> constraint : sci.getConstraints()
					.entrySet()) {
				Object[] values = constraint.getValue().toArray();
				if (values == null || values.length == 0)
					continue;
				String fieldName = constraint.getKey();
				if (programName != null
						&& fieldAlreadyIndexedInProgram(programName, fieldName)) {
					continue;
				}
				if (!SolrFieldName.isNameReserved(fieldName)) {
					fieldName = addSolrDynamicFieldExtension(
							constraint.getKey(), values);
				}
				doc.addField(fieldName, values);
				// Now add for the text indexing dynamic field
				addIndexedTextDynamicField(doc, fieldName, values);
			}
		}
	}

	private void addSolrSpecificFields(SolrContentItem sci,
			SolrInputDocument doc, String ldProgramName) {
		doc.addField(SolrFieldName.TITLE.toString(), sci.getTitle());
		addConstraints(sci, doc, ldProgramName);
		try {
			Iterator<Triple> it = sci.getMetadata().filter(null,
					Properties.ENHANCER_ENTITY_REFERENCE, null);
			Set<String> contexts = new HashSet<String>();
			while (it.hasNext()) {
				Resource r = it.next().getObject();
				if (r instanceof UriRef) {
					contexts.add(((UriRef) r).getUnicodeString());
				}
			}
			Map<String, Collection<?>> results = semanticIndexManager
					.executeProgram(ldProgramName, contexts);
			for (Entry<String, Collection<?>> entry : results.entrySet()) {
				doc.addField(entry.getKey(), entry.getValue());
			}
		} catch (LDPathException e) {
			log.error(
					"Cannot execute the ldPathProgram on SolrContentItem's metadata",
					e);
		}
	}

	private void addSolrSpecificFields(SolrContentItem sci,
			SolrInputDocument doc) {
		doc.addField(SolrFieldName.TITLE.toString(), sci.getTitle());
		addConstraints(sci, doc, null);
		if (sci.getMetadata() != null) {
			addSemanticFields(sci, doc);
			addAnnotatedEntityFieldNames(sci, doc);
		} else {
			log.debug("There are no enhancements for the content item {}", sci
					.getUri().getUnicodeString());
		}
	}

	private void addSemanticFields(SolrContentItem sci, SolrInputDocument doc) {
		for (SolrFieldName fn : SolrFieldName.getSemanticFieldNames()) {
			addField(sci, doc, fn);
		}
	}

	private void addAnnotatedEntityFieldNames(SolrContentItem sci,
			SolrInputDocument doc) {
		for (SolrFieldName fn : SolrFieldName.getAnnotatedEntityFieldNames()) {
			addField(sci, doc, fn);
		}
	}

	private void addField(SolrContentItem sci, SolrInputDocument doc,
			SolrFieldName fieldName) {
		SelectQuery query = null;
		try {
			query = (SelectQuery) QueryParser.getInstance().parse(
					QueryGenerator.getFieldQuery(fieldName));
		} catch (ParseException e) {
			log.debug("Should never reach here!");
			log.error("Cannot parse the query generated by QueryGenerator: {}",
					QueryGenerator.getFieldQuery(fieldName), e);
			return;
		}

		sci.getMetadata();
		ResultSet result = tcManager.executeSparqlQuery(query,
				sci.getMetadata());
		List<String> values = new ArrayList<String>();
		while (result.hasNext()) {
			SolutionMapping sol = result.next();
			Resource res = sol.get(fieldName.toString());
			if (res == null)
				continue;
			String value = res.toString();
			if (res instanceof Literal) {
				value = ((Literal) res).getLexicalForm();
			}
			value = value.replaceAll("_", " ");
			values.add(value);
		}
		if (!values.isEmpty()) {
			String fn = fieldName.toString();
			Object[] valArr = values.toArray();
			doc.addField(fn, valArr);
			// Now add for the text indexing dynamic field
			addIndexedTextDynamicField(doc, fn, valArr);
		}
	}

	private void addIndexedTextDynamicField(SolrInputDocument doc, String fn,
			Object[] valArr) {
		if (fn.endsWith(SolrVocabulary.SOLR_DYNAMIC_FIELD_TEXT)) {
			// replace the last "_t" with "_i"
			String ifn = SolrVocabulary.STANBOLRESERVED_PREFIX
					+ fn.substring(
							0,
							fn.lastIndexOf(SolrVocabulary.SOLR_DYNAMIC_FIELD_TEXT))
					+ SolrVocabulary.SOLR_DYNAMIC_FIELD_INDEXEDTEXT;
			doc.addField(ifn, valArr);
		}
	}

	@Override
	public ContentItem get(String id) throws StoreException {
		return get(id, SolrCoreManager.CONTENTHUB_DEFAULT_INDEX_NAME);
	}

	// TODO: we can use cache for "Recently uploaded Content Items"..
	@Override
	public SolrContentItem get(String id, String ldProgramName)
			throws StoreException {
		id = ContentItemIDOrganizer.attachBaseURI(id);
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext,
				managedSolrServer).getServer(ldProgramName);
		byte[] content = null;
		String mimeType = null;
		String title = null;
		Map<String, List<Object>> constraints = new HashMap<String, List<Object>>();

		SolrQuery query = new SolrQuery();
		StringBuilder queryString = new StringBuilder();
		queryString.append(SolrFieldName.ID.toString());
		queryString.append(":\"");
		queryString.append(id);
		queryString.append('\"');
		query.setQuery(queryString.toString());
		QueryResponse response;
		try {
			response = solrServer.query(query);
			SolrDocumentList results = response.getResults();
			if (results != null && results.size() > 0) {
				SolrDocument result = results.get(0);
				content = (byte[]) result
						.getFieldValue(SolrFieldName.BINARYCONTENT.toString());
				mimeType = (String) result.getFieldValue(SolrFieldName.MIMETYPE
						.toString());
				title = (String) result.getFieldValue(SolrFieldName.TITLE
						.toString());

				Iterator<Entry<String, Object>> itr = result.iterator();
				while (itr.hasNext()) {
					Entry<String, Object> entry = itr.next();
					String key = entry.getKey();
					if (!SolrFieldName.isNameReserved(key)) {
						List<Object> values = (List<Object>) result
								.getFieldValues(key);
						constraints.put(key, values);
					}
				}
			} else {
				log.warn("No matching item in Solr for the given id {}.", id);
				return null;
			}
		} catch (SolrServerException ex) {
			log.error("", ex);
			throw new StoreException(ex.getMessage(), ex);
		}

		String enhancementQuery = QueryGenerator.getEnhancementsOfContent(id);
		SelectQuery selectQuery = null;
		try {
			selectQuery = (SelectQuery) QueryParser.getInstance().parse(
					enhancementQuery);
		} catch (ParseException e) {
			String msg = "Cannot parse the SPARQL while trying to retrieve the enhancements of the ContentItem";
			log.error(msg, e);
			throw new StoreException(msg, e);
		}

		ResultSet resultSet = tcManager.executeSparqlQuery(selectQuery,
				this.getEnhancementGraph());
		MGraph metadata = new SimpleMGraph();
		while (resultSet.hasNext()) {
			SolutionMapping mapping = resultSet.next();
			UriRef ref = (UriRef) mapping.get("enhID");
			Iterator<Triple> tripleItr = this.getEnhancementGraph().filter(ref,
					null, null);
			while (tripleItr.hasNext()) {
				Triple triple = tripleItr.next();
				metadata.add(triple);
			}
		}

		return new SolrContentItemImpl(id, title, content, mimeType, metadata,
				constraints);
	}

	@Override
	public void deleteById(String id, String ldProgramName)
			throws StoreException {
		if (id == null || id.isEmpty())
			return;
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext,
				managedSolrServer).getServer(ldProgramName);
		id = ContentItemIDOrganizer.attachBaseURI(id);
		removeEnhancements(id);
		try {
			solrServer.deleteById(id);
			solrServer.commit();
		} catch (SolrServerException e) {
			log.error("Solr Server Exception", e);
			throw new StoreException(e.getMessage(), e);
		} catch (IOException e) {
			log.error("IOException", e);
			throw new StoreException(e.getMessage(), e);
		}
	}

	@Override
	public void deleteById(String id) throws StoreException {
		deleteById(id, SolrCoreManager.CONTENTHUB_DEFAULT_INDEX_NAME);
	}

	@Override
	public void deleteById(List<String> idList, String ldProgramName)
			throws StoreException {
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext,
				managedSolrServer).getServer(ldProgramName);
		for (int i = 0; i < idList.size(); i++) {
			String id = ContentItemIDOrganizer.attachBaseURI(idList.get(i));
			idList.remove(i);
			idList.add(i, id);
		}
		try {
			solrServer.deleteById(idList);
			solrServer.commit();
		} catch (SolrServerException e) {
			log.error("Solr Server Exception", e);
			throw new StoreException(e.getMessage(), e);
		} catch (IOException e) {
			log.error("IOException", e);
			throw new StoreException(e.getMessage(), e);
		}
	}

	@Override
	public void deleteById(List<String> idList) throws StoreException {
		deleteById(idList, SolrCoreManager.CONTENTHUB_DEFAULT_INDEX_NAME);
	}

}
