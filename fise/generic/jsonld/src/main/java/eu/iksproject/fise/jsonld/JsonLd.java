package eu.iksproject.fise.jsonld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author Fabian Christ
 * 
 */
public class JsonLd {

	// Map Namespace -> Prefix
	private Map<String, String> namespacePrefixMap = new HashMap<String, String>();

	// Map Subject -> Resource
	private Map<String, JsonLdResource> resourceMap = new TreeMap<String, JsonLdResource>(new JsonComparator());

	/**
	 * Flag to control whether the namespace prefix map should be used
	 * to shorten IRIs to prefix notation during serialization. Default
	 * value is <code>true</code>.<br />
	 * <br />
	 * If you already put values into this JSON-LD instance with prefix
	 * notation, you should set this to <code>false</code> before starting
	 * the serialization.
	 */
	private boolean applyNamespaces = true;
	
	/**
	 * Flag to control whether the serialized JSON-LD output will use
	 * joint or disjoint graphs for subjects and namespaces.  Default
	 * value is <code>true</code>.
	 */
	private boolean useJointGraphs = true;
	
	/**
	 * Add the given resource to this JsonLd object using the resourceId
	 * as key.
	 * 
	 * @param resourceId
	 * @param resource
	 */
	public void put(String resourceId, JsonLdResource resource) {
		this.resourceMap.put(resourceId, resource);
	}

	@Override
	public String toString() {
		if (useJointGraphs) {
			Map<String, Object> json = createJointGraph();
			
			return JSONObject.toJSONString(json);
		}
		else {
			JSONArray json = createDisjointGraph();
			
			return JSONArray.toJSONString(json);
		}
	}
	
	private JSONArray createDisjointGraph() {
		JSONArray json = new JSONArray();
		if (this.resourceMap.size() > 0) {

			for (String subject : this.resourceMap.keySet()) {
				Map<String, Object> subjectObject = new TreeMap<String, Object>(new JsonComparator());

				// put the namespaces
				if (this.namespacePrefixMap.size() > 0) {
					Map<String, Object> nsObject = new TreeMap<String, Object>(new JsonComparator());
					for (String ns : this.namespacePrefixMap.keySet()) {
						nsObject.put(this.namespacePrefixMap.get(ns), ns);
					}
					subjectObject.put("#", nsObject);
				}

				JsonLdResource resource = this.resourceMap.get(subject);

				// put subject
				if (resource.getSubject() != null) {
					subjectObject.put("@", resource.getSubject());
				}
				
				// put types
				putTypes(subjectObject, resource);

				// put properties = objects
				putProperties(subjectObject, resource);

				// add to list of subjects
				json.add(subjectObject);
			}

		}
		
		return json;
	}

	private Map<String, Object> createJointGraph() {
		Map<String, Object> json = new TreeMap<String, Object>(new JsonComparator());
		if (this.resourceMap.size() > 0) {
			JSONArray subjects = new JSONArray();

			for (String subject : this.resourceMap.keySet()) {
				// put subject
				Map<String, Object> subjectObject = new TreeMap<String, Object>(new JsonComparator());

				JsonLdResource resource = this.resourceMap.get(subject);

				// put subject
				if (resource.getSubject() != null) {
					subjectObject.put("@", resource.getSubject());
				}
				
				// put types
				putTypes(subjectObject, resource);

				// put properties = objects
				putProperties(subjectObject, resource);

				// add to list of subjects
				subjects.add(subjectObject);
			}

			// put subjects
			if (subjects.size() > 0) {
				if (subjects.size() == 1) {
					json = (Map) subjects.get(0);
				} else {
					json.put("@", subjects);
				}
			}
		}

		// put the namespaces
		if (this.namespacePrefixMap.size() > 0) {
			Map<String, Object> nsObject = new TreeMap<String, Object>(new JsonComparator());
			for (String ns : this.namespacePrefixMap.keySet()) {
				nsObject.put(this.namespacePrefixMap.get(ns), ns);
			}
			json.put("#", nsObject);
		}
		
		return json;
	}

	@SuppressWarnings("unchecked")
	private void putTypes(Map<String, Object> subjectObject, JsonLdResource resource) {
		if (resource.getTypes().size() > 0) {
			JSONArray types = new JSONArray();
			for (String type : resource.getTypes()) {
				types.add(this.applyNamespace(type));
			}
			if (types.size() == 1) {
				subjectObject.put("a", types.get(0));
			}
			else {
				Collections.sort(types, new Comparator<String>() {

					@Override
					public int compare(String arg0, String arg1) {
						return arg0.compareTo(arg1);
					}

				});
				subjectObject.put("a", types);
			}
		}
	}
	
	private void putProperties(Map<String, Object> jsonObject, JsonLdResource resource) {
		for (String property : resource.getPropertyMap().keySet()) {
			Object value = resource.getPropertyMap().get(property);
			if (value instanceof String) {
				value = this.applyNamespace((String) value);
				jsonObject.put(this.applyNamespace(property), value);
			}
			else if (value instanceof String[]) {
				String[] stringArray = (String[]) value;
				List<String> valueList = new ArrayList<String>();
				for (int i=0; i<stringArray.length; i++) {
					valueList.add(this.applyNamespace(stringArray[i]));
				}
				JSONArray jsonArray = new JSONArray();
				jsonArray.addAll(valueList);
				jsonObject.put(this.applyNamespace(property), jsonArray);
			}
			else if (value instanceof Object[]) {
				Object[] objectArray = (Object[]) value;
				JSONArray jsonArray = new JSONArray();
				for (Object object : objectArray) {
					jsonArray.add(object);
				}
				jsonObject.put(this.applyNamespace(property), jsonArray);
			}
			else {
				jsonObject.put(this.applyNamespace(property), value);
			}
		}
	}

	private String applyNamespace(String uri) {
		if (this.applyNamespaces) {
			for (String namespace : this.namespacePrefixMap.keySet()) {
				String prefix = this.namespacePrefixMap.get(namespace) + ":";
				uri = uri.replaceAll(namespace, prefix);
			}
		}
		return uri;
	}

	/**
	 * Return the JSON-LD Resource for the given subject.
	 * 
	 * @param subject
	 * @return
	 */
	public JsonLdResource getResource(String subject) {
		return this.resourceMap.get(subject);
	}

	/**
	 * Get the known namespace to prefix mapping.
	 * 
	 * @return A {@link java.util.Map} from namespace String to prefix String.
	 */
	public Map<String, String> getNamespacePrefixMap() {
		return namespacePrefixMap;
	}

	/**
	 * Sets the known namespaces for the serializer.
	 * 
	 * @param namespacePrefixMap
	 *            A {@link java.util.Map} from namespace String to prefix
	 *            String.
	 */
	public void setNamespacePrefixMap(Map<String, String> namespacePrefixMap) {
		this.namespacePrefixMap = namespacePrefixMap;
	}

	/**
	 * Adds a new namespace and its prefix to the list of used namespaces for this
	 * JSON-LD instance.
	 * 
	 * @param namespace A namespace IRI.
	 * @param prefix A prefix to use and identify this namespace in serialized JSON-LD.
	 */
	public void addNamespacePrefix(String namespace, String prefix) {
		this.namespacePrefixMap.put(namespace, prefix);
	}

	/**
	 * Determine whether currently joint or disjoint graphs are serialized with this JSON-LD instance.
	 * 
	 * @return <code>True</code> if joint graphs are used, <code>False</code>otherwise.
	 */
	public boolean isUseJointGraphs() {
		return useJointGraphs;
	}

	/**
	 * Set to <code>true</code> if you want to use joint graphs (default) or <code>false</code> otherwise.
	 * 
	 * @param useJointGraphs
	 */
	public void setUseJointGraphs(boolean useJointGraphs) {
		this.useJointGraphs = useJointGraphs;
	}

	/**
	 * Flag to control whether the namespace prefix map should be used
	 * to shorten IRIs to prefix notation during serialization. Default
	 * value is <code>true</code>.<br />
	 * <br />
	 * If you already put values into this JSON-LD instance with prefix
	 * notation, you should set this to <code>false</code> before starting
	 * the serialization.
	 *
	 * @return <code>True</code> if namespaces are applied during serialization, <code>false</code> otherwise.
	 */
	public boolean isApplyNamespaces() {
		return applyNamespaces;
	}

	/**
	 * Control whether namespaces from the namespace prefix map are
	 * applied to URLs during serialization.<br />
	 * <br />
	 * Set this to <code>false</code> if you already have shortened IRIs
	 * with prefixes.
	 * 
	 * @param applyNamespaces
	 */
	public void setApplyNamespaces(boolean applyNamespaces) {
		this.applyNamespaces = applyNamespaces;
	}
	
}
