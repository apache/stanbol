package eu.iksproject.kres.jersey;

import static org.junit.Assert.*;

import java.io.File;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.representation.Form;

import eu.iksproject.kres.api.format.KReSFormat;

public class JettyServerTest {

	public static final int __PORT = 9999;

	public static final String __TEST_URI = "http://localhost:" + __PORT + "/";

	public static final String _ROOT_URI = __TEST_URI + "ontology";

	public static final String ONT_FOAF_URI = "http://xmlns.com/foaf/spec/index.rdf";

	public static final String ONT_PIZZA_URI = "http://www.co-ode.org/ontologies/pizza/2007/02/12/pizza.owl";

	public static final String ONT_WINE_URI = "http://www.schemaweb.info/webservices/rest/GetRDFByID.aspx?id=62";

	public static final String REG_TEST_URI = "http://www.ontologydesignpatterns.org/registry/krestest.owl";

	public static final String SCOPE_BIZ_URI = _ROOT_URI + "/" + "Biz";

	public static final String SCOPE_DRUNK_URI = _ROOT_URI + "/" + "Drunk";

	public static final String SCOPE_USER_URI = _ROOT_URI + "/" + "User";

	public static final String SCOPE1_URI = _ROOT_URI + "/" + "Pippo%20Baudo";

	public static final String SCOPE2_URI = _ROOT_URI + "/" + "TestScope2";

	private static JettyServer server;

	@BeforeClass
	public static void startServer() throws Exception {
		server = new JettyServer();
		server.start(__TEST_URI);
	}

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
	}

	private Client client;

	private WebResource ontologyResource, scopeResourceTest1,
			scopeResourceTest2;

	@Before
	public void setUp() throws Exception {

		// simulate OSGi runtime by registering the components to test manually

		server.setAttribute("", null);

		// Serializer serializer = new Serializer();
		// serializer.bindSerializingProvider(new JenaSerializerProvider());
		// serializer.bindSerializingProvider(new RdfJsonSerializingProvider());
		// server.setAttribute(Serializer.class.getName(), serializer);
		//
		// TcManager tcManager = new TcManager();
		// server.setAttribute(TcManager.class.getName(), tcManager);

		// server.setAttribute(
		// FreemarkerViewProcessor.FREEMARKER_TEMPLATE_PATH_INIT_PARAM,
		// "/META-INF/templates");

		client = Client.create();
		ontologyResource = client.resource(_ROOT_URI);
		scopeResourceTest1 = client.resource(SCOPE1_URI);
		scopeResourceTest2 = client.resource(SCOPE2_URI);

	}

	@Test
	public void testEcho() throws Exception {

		boolean eq = true;

		Client client = Client.create();
		WebResource resUpload = client.resource(__TEST_URI + "prova");

		resUpload.get(String.class);
		ClientResponse head = resUpload.head();
		int status = head.getStatus();
		head.close();
		eq &= status == Status.OK.getStatusCode();

		resUpload = client.resource(__TEST_URI + "prova/saluto");

		resUpload.get(String.class);
		head = resUpload.head();
		status = head.getStatus();
		head.close();
		eq &= status == Status.OK.getStatusCode();

		client.destroy();

		assertTrue(eq);

	}

	public void testOntologyUpload() throws Exception {
		WebResource resUpload = client.resource(_ROOT_URI + "/upload");

		Form f = new Form();
		f
				.add("file", new File(
						"./src/main/resources/TestFile/ProvaParent.owl"));

		//resUpload.type(MediaType.MULTIPART_FORM_DATA).post();

		assertTrue(true);
	}

	/**
	 * Tests that the creation of active and inactive scopes is reflected in the
	 * RDF version of the scope set, whether it is set to display all scopes or
	 * only the active ones.
	 * 
	 * @throws Exception
	 */
	// @Test
	public void testActiveVsAll() throws Exception {
		// The needed Web resources to GET from.
		WebResource resActive = client.resource(_ROOT_URI);
		WebResource resAllScopes = client.resource(_ROOT_URI
				+ "?with-inactive=true");
		// Put a simple, inactive scope.
		client.resource(SCOPE_USER_URI + "?coreont=" + ONT_FOAF_URI).put(
				String.class);
		// Check that it is in the list of all scopes.
		String r = resAllScopes.get(String.class);
		assertTrue(r.contains(SCOPE_USER_URI));
		// Check that it is not in the list of active scopes.
		r = resActive.get(String.class);
		assertTrue(!r.contains(SCOPE_USER_URI));
		// Now create a scope that is active on startup.
		client.resource(
				SCOPE_BIZ_URI + "?coreont=" + ONT_PIZZA_URI + "&activate=true")
				.put(String.class);
		// Check that it appears in both sets.
		r = resAllScopes.get(String.class);
		assertTrue(r.contains(SCOPE_BIZ_URI));
		r = resActive.get(String.class);
		assertTrue(r.contains(SCOPE_BIZ_URI));
	}

	@Test
	public void testGetScopes() throws Exception {
		String r = ontologyResource.accept(KReSFormat.RDF_XML)
				.get(String.class);
		assertTrue(r
				.contains("<imports rdf:resource=\"http://www.ontologydesignpatterns.org/schemas/meta.owl\"/>"));
		r = ontologyResource.accept(KReSFormat.TURTLE).get(String.class);
		assertTrue(r
				.contains("[ owl:imports <http://www.ontologydesignpatterns.org/schemas/meta.owl>\n] ."));
	}

	@Test
	public void testRemoval() throws Exception {
		String wineId = "http://www.w3.org/TR/2003/WD-owl-guide-20030331/wine";
		client.resource(SCOPE_DRUNK_URI + "?corereg=" + REG_TEST_URI).put(
				String.class);
		// // Request entities in DELETE methods are unsupported...
		// Form f = new Form();
		// f.add("ontology",
		// "http://www.w3.org/TR/2003/WD-owl-guide-20030331/wine");
		// drunkRes.delete(f);

		client.resource(SCOPE_DRUNK_URI + "?ontology=" + wineId).delete();
		assertTrue(true);
	}

	// @Test
	public void testLocking() throws Exception {
		// Create a scope with a core ontology and a custom registry.
		String r;
		// String PIZZA_URI =
		// "http://www.co-ode.org/ontologies/pizza/2007/02/12/pizza.owl";
		WebResource ts2res = client.resource(SCOPE2_URI + "?customont="
				+ ONT_PIZZA_URI + "&corereg=" + REG_TEST_URI);
		ts2res.put();
		r = scopeResourceTest2.accept(KReSFormat.RDF_XML).get(String.class);
		// System.err.println(r);
		// Now add an ontology
		try {
			Form f = new Form();
			f.add("location", ONT_PIZZA_URI);
			f.add("registry", "false");
			scopeResourceTest2.post(String.class, f);
			r = scopeResourceTest2.accept(KReSFormat.RDF_XML).get(String.class);
			// fail("Addition succeded on existing scope with supposedly locked core space!");
		} catch (WebApplicationException ex) {
			assertTrue(r != null);
			return;
		} catch (Exception ex) {
			assertTrue(r != null);
			return;
		}

	}

	// @Test
	public void testSessionCreation() {
		WebResource resource = client.resource(__TEST_URI + "session");
		String r = resource.accept(KReSFormat.RDF_XML).post(String.class);
		// System.err.println(r);
		assertTrue(true);
	}

	// @Test
	public void testScopeManagement() throws Exception {
		String rootIdToken = "rdf:about=\"http://localhost:9999/ontology/Pippo%20Baudo/custom/root.owl\"";
		String importORToken = "<owl:imports rdf:resource=\"http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl\"/>";
		// Create the new scope with a supplied ontology registry.
		String r = client.resource(
				SCOPE1_URI + "?coreont=http://xmlns.com/foaf/spec/index.rdf"
				// + "?corereg="+REG_TEST_URI
						// + "&customreg="+REG_TEST_URI
						+ "&activate=true").put(String.class);
		// Check that it appears in the scope set
		r = ontologyResource.accept(KReSFormat.RDF_XML).get(String.class);
		// System.err.println(r);
		assertTrue(r.contains(SCOPE1_URI)
				&& r
						.contains("rdf:type rdf:resource=\"http://kres.iks-project.eu/ontology/onm/meta.owl#Scope\""));
		// Check that the top ontology has the correct ID and imports objectrole
		r = scopeResourceTest1.accept(KReSFormat.RDF_XML).get(String.class);
		// System.err.println(r);
		assertTrue(r.contains(rootIdToken));
		assertTrue(r.contains("http://xmlns.com/foaf/spec/index.rdf"));
		// Now add an ontology
		Form f = new Form();
		f.add("location",
				"http://www.co-ode.org/ontologies/pizza/2007/02/12/pizza.owl");
		f.add("registry", "false");
		scopeResourceTest1.post(String.class, f);
		r = scopeResourceTest1.accept(KReSFormat.RDF_XML).get(String.class);
		// System.err.println(r);
	}
}
