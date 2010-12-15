package eu.iksproject.fise.engines.opencalais.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.fise.servicesapi.ContentItem;
import eu.iksproject.fise.servicesapi.EngineException;
import eu.iksproject.fise.servicesapi.EnhancementEngine;
import eu.iksproject.fise.servicesapi.InvalidContentException;
import eu.iksproject.fise.servicesapi.ServiceProperties;
import eu.iksproject.fise.servicesapi.helper.EnhancementEngineHelper;
import eu.iksproject.fise.servicesapi.rdf.Properties;

/**
 * This class provides an interface to the OpenCalais service for Named Entity Recognition. 
 * It uses the OpenCalais REST service with the 'paramsXML' structures for passing
 * parameters {@link http://www.opencalais.com/documentation/calais-web-service-api/api-invocation/rest-using-paramsxml)}.
 * 
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 */

@Component(immediate = true, metatype = true)
@Service
public class OpenCalaisEngine implements EnhancementEngine, ServiceProperties {

  private static Logger log = LoggerFactory.getLogger(OpenCalaisEngine.class);

  /**
   * This contains the directly supported MIME types of this enhancement engine. For handling other mime-types the plain text must be contained in the metadata as by Metaxa.
   */
  protected static final List<String> SUPPORTED_MIMETYPES = Arrays.asList(new String[]{"text/plain", "text/html"});

  /**
   * This contains a list of languages supported by OpenCalais.
   * If the metadata don't contain a value for the language as the value of the {@link Property.DC_LANG property}
   * it is left to the grace of the OpenCalais whether it accepts the text.
   * OpenCalais uses its own language identifcation anyway.
   */
  protected static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(new String[]{"en","fr","es"});

  /**
   * The default value for the Execution of this Engine. Currently set to
   * {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT} + 10. It should run after Metaxa and LangId.
   */
  public static final Integer defaultOrder = ServiceProperties.ORDERING_EXTRACTION_ENHANCEMENT+10;

  @Property
  public static final String LICENSE_KEY = "eu.iksproject.fise.engines.opencalais.license";
  
  @Property(value="http://api.opencalais.com/enlighten/rest/")
  public static final String CALAIS_URL_KEY = "eu.iksproject.fise.engines.opencalais.url";

  /**
   * the URL for the Calais REST Service
   */
  private String calaisUrl = "http://api.opencalais.com/enlighten/rest/";
  
  /**
   * the license key from OpenCalais for using the service
   */
  private String licenseKey = null;

  @Reference
  TcManager tcManager;

  BundleContext bundleContext;
    
  public String getLicenseKey() {
    return licenseKey;
  }

  public void setLicenseKey(String licenseKey) {
    this.licenseKey = licenseKey;
  }

  public String getCalaisUrl() {
    return calaisUrl;
  }

  public void setCalaisUrl(String calaisUrl) {
    this.calaisUrl = calaisUrl;
  }

  public Map<String, Object> getServiceProperties() {
    // TODO Auto-generated method stub
    return Collections.unmodifiableMap(Collections.singletonMap(
        ServiceProperties.ENHANCEMENT_ENGINE_ORDERING,
        (Object) defaultOrder));
  }

  /**
   * {@inheritDoc}
   */
  public int canEnhance(ContentItem ci) throws EngineException {
    if (getLicenseKey() == null || getLicenseKey().trim().length()==0) {
      //do nothing if no license key is defined
      log.warn("No license key defined. The engine will not work!");
      return CANNOT_ENHANCE;
    }
    UriRef subj = new UriRef(ci.getId());
    String mimeType = ci.getMimeType().split(";",2)[0];
    if (SUPPORTED_MIMETYPES.contains(mimeType.toLowerCase())) {
      // check language
      String language = getMetadataLanguage(ci.getMetadata(),null);
      if (language != null && !SUPPORTED_LANGUAGES.contains(language)) {
        log.warn("Wrong language for Calais: {}",language);
        return CANNOT_ENHANCE;
      }
      return ENHANCE_SYNCHRONOUS;
    }
    else {
      // TODO: check whether the metadata graph contains the text
      Iterator<Triple> it = ci.getMetadata().filter(subj, Properties.NIE_PLAINTEXTCONTENT, null);
      if (it.hasNext()) {
        return ENHANCE_SYNCHRONOUS;
      }
    }
    return CANNOT_ENHANCE;
  }

  /**
   * {@inheritDoc}
   */
  public void computeEnhancements(ContentItem ci) throws EngineException {
    String text = "";
    if (SUPPORTED_MIMETYPES.contains(ci.getMimeType().split(";",2)[0].toLowerCase())) {
      try {
        text = IOUtils.toString(ci.getStream());
      } catch (IOException e) {
        throw new InvalidContentException(this, ci, e);
      }
    }
    else {
      text = getMetadataText(ci.getMetadata(), new UriRef(ci.getId()));
    }
    if (text == null) {
        log.warn("no text found");
        return;
    }

    MGraph calaisModel = getCalaisAnalysis(text,ci);
    if (calaisModel != null) {
    	createEnhancements(queryModel(calaisModel),ci);
    }
    
  }

  /**
   * This generates enhancement structures for the entities from OpenCalais and adds them to the content item's metadata. For each entity a TextAnnotation and an EntityAnnotation are created. An EntityAnnotation can relate to several TextAnnotations.
   * 
   * @param occs a Collection of entity information
   * @param ci the content item
   */
  public void createEnhancements(Collection<CalaisEntityOccurrence> occs, ContentItem ci) {
  	LiteralFactory literalFactory = LiteralFactory.getInstance();
  	//TODO create TextEnhancement (form, start, end, type?) and EntityAnnotation (id, name, type)
  	HashMap<Resource, UriRef> entityAnnotationMap = new HashMap<Resource,UriRef>();
    for (CalaisEntityOccurrence occ: occs) {
      UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(
          ci, this);
      MGraph model = ci.getMetadata();
      model.add(new TripleImpl(textAnnotation, Properties.DC_TYPE, occ.type));
      model.add(new TripleImpl(textAnnotation,Properties.FISE_SELECTED_TEXT, occ.exact));
      model.add(new TripleImpl(textAnnotation,Properties.FISE_START, literalFactory.createTypedLiteral(occ.offset)));
      model.add(new TripleImpl(textAnnotation,Properties.FISE_END, literalFactory.createTypedLiteral(occ.offset + occ.length)));
      model.add(new TripleImpl(textAnnotation,Properties.FISE_SELECTED_TEXT, occ.exact));
      model.add(new TripleImpl(textAnnotation,Properties.FISE_SELECTION_CONTEXT, literalFactory.createTypedLiteral(occ.context)));
      //create EntityAnnotation only once but add a reference to the textAnnotation
      if (entityAnnotationMap.containsKey(occ.id)) {
        model.add(new TripleImpl(entityAnnotationMap.get(occ.id),Properties.DC_RELATION, textAnnotation));
      }
      else {
        UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(ci, this);
        entityAnnotationMap.put(occ.id, entityAnnotation);
        model.add(new TripleImpl(entityAnnotation,Properties.DC_RELATION, textAnnotation));
        model.add(new TripleImpl(entityAnnotation,Properties.FISE_ENTITY_LABEL, occ.name));
        model.add(new TripleImpl(entityAnnotation,Properties.FISE_ENTITY_TYPE, occ.type));
        model.add(new TripleImpl(entityAnnotation,Properties.FISE_ENTITY_REFERENCE, occ.id));
      }
    }
  }

  /**
   * This retrieves the annotations from OpenCalais as RDF/XML. From that an MGraph is created.
   * @param text the text to send to OpenCalais
   * @return an MGraph with all annotations
   * @throws EngineException
   */
  public MGraph getCalaisAnalysis(String text,ContentItem ci) throws EngineException {
    String mimeType = ci.getMimeType().split(";",2)[0].toLowerCase();
    if (mimeType.equals("text/plain")) {
      mimeType = "text/raw";
    }
    String calaisParams = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
    "<c:processingDirectives c:contentType=\""+mimeType+"\" " +
    //    "c:enableMetadataType=\"GenericRelations,SocialTags\" "+
    //    "c:enableMetadataType=\"GenericRelations\" "+
    "c:outputFormat=\"rdf/xml\" "+
    "c:calculateRelevanceScore=\"false\" "+
    "c:omitOutputtingOriginalText=\"true\""+
    ">"+
    "</c:processingDirectives>"+
    "</c:params>";
    MGraph model = null;
    try {
      StringBuilder postParams = new StringBuilder();
      postParams
      .append("licenseID=")
      .append(URLEncoder.encode(getLicenseKey(), "UTF-8"))
      .append("&content=")
      .append(URLEncoder.encode(text, "UTF-8"))
      .append("&paramsXML=")
      .append(URLEncoder.encode(calaisParams, "UTF-8"));
      // get annotations from Calais
      String calaisResult = 
        doPostRequest(
            this.getCalaisUrl(), null, postParams.toString(),
            "application/x-www-form-urlencoded", "UTF-8");
      log.debug("Calais data:\n{}",calaisResult);
      // build model from Calais result
      InputStream in = new ByteArrayInputStream(calaisResult.getBytes("utf-8"));
      model = readModel(in,"application/rdf+xml");
    } catch (UnsupportedEncodingException e) {
    	throw new EngineException(e.getMessage(), e);
    } catch (IOException e) {
      throw new EngineException(e.getMessage(), e);
    }
    return model;
  }

  /**
   * This parses an InputStream of RDF data and produces an MGraph from them
   * @param in The InputStream of RDF data
   * @param format the format of the RDF data
   * @return the resulting MGraph or null if the RDF serialization format is not supported by the parser
   */
  public MGraph readModel(InputStream in, String format) {
    Parser parser = Parser.getInstance();
    if (parser.getSupportedFormats().contains(format)) {
      Graph graph = parser.parse(in, format);
      MGraph model = new SimpleMGraph(graph);
      return model;
    }
    else {
      log.warn("Unsupported RDF format: {}\nSupported RDF formats: {}",format,parser.getSupportedFormats());      
    }
    return null;
  }

  /**
   * This extracts the relevant entity information from the Calais RDF data. The entities and the relted information is extracted by a Sparql query.
   * @param model the MGraph representing the Calais data
   * @return a Collection of entity information
   */
  public Collection<CalaisEntityOccurrence> queryModel(MGraph model) {
    //TODO extract also Geo info (latitude/longitude)?
    String query =
    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + 
    "PREFIX p: <http://s.opencalais.com/1/pred/> " +
    "PREFIX t: <http://s.opencalais.com/1/type/em/e/> " +  
    "SELECT DISTINCT ?id ?did ?name ?type ?dtype ?offset ?length ?exact ?context ?score WHERE { " +
    "?id p:name ?name ." +
    "?id rdf:type ?type ." +
    "?y p:subject ?id ." +
    "?y p:offset ?offset ." +
    "?y p:length ?length ." +
    "?y p:exact ?exact ." +
    "?y p:detection ?context ." +
    " OPTIONAL { ?z p:subject ?id . ?z p:relevance ?score . } " +
    // get disambiguated entity references if available
    " OPTIONAL { ?did p:subject ?id . ?did p:name ?name . ?did rdf:type ?dtype . } " +
    "FILTER (" +
    "?type = t:Person || " +
    "?type = t:City || " +
    "?type = t:Continent || " +
    "?type = t:Country || " +
    "?type = t:ProvinceOrState || " +
    "?type = t:Region || " +
    "?type = t:Company || " +
    "?type = t:Facility || " +
    "?type = t:Organization " + 
    ")" +
    "} ";
    Collection<CalaisEntityOccurrence> result = new ArrayList<CalaisEntityOccurrence>();
    try {
      SelectQuery sQuery = (SelectQuery)QueryParser.getInstance().parse(query);
      ResultSet rs = tcManager.executeSparqlQuery(sQuery, model);
      while(rs.hasNext()) {
        SolutionMapping row = rs.next();
        CalaisEntityOccurrence occ = new CalaisEntityOccurrence();
        Resource disambiguated = row.get("did");
        occ.id = (disambiguated == null?row.get("id"):disambiguated);
        occ.type = (disambiguated == null?row.get("type"):row.get("dtype"));
        occ.name = row.get("name");
        occ.exact = row.get("exact");
        //TODO for html the offsets might not be those of the original document but refer to a cleaned up version?
        occ.offset = Integer.valueOf(((Literal)row.get("offset")).getLexicalForm());
        // remove brackets
        occ.context = ((Literal)row.get("context")).getLexicalForm().replaceAll("[\\[\\]]", "");
        occ.length = Integer.valueOf(((Literal)row.get("length")).getLexicalForm());
        if (row.get("score") != null) {
        	occ.relevance = Double.valueOf(((Literal)row.get("score")).getLexicalForm());
        }
        result.add(occ);
      }
     } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return result;
  }

  
  /**
   * This sends a POST request to the given url. 
   *
   * @param targetUrl a <code>String</code> with the target url
   * @param params a <code>Map<String,String></code> object containing the url parameters; use <code>null</code> if there are no parameters 
   * @param body a <code>String</code> with the body of the post request; use
   * <code>null</code> if the body is empty
   * @param contentType a <code>String</code> with the content type of the post
   * request; use <code>null</code> for the default content type 
   * <code>text/xml; charset=utf-8</code>
   * @param responseEncoding a <code>String</code> with the encoding used to
   * read the server response; use <code>null</code> for the default charset
   * @return a <code>String</code> with the server response
   * @throws IOException if an error occurs
   */
  public static String doPostRequest(
      String targetUrl, Map<String,String> params, String body, String contentType,
      String responseEncoding) 
    throws IOException {
    
    StringBuilder urlString = new StringBuilder(targetUrl);

    // add parameters to url
    if (params != null) {
      if (!params.isEmpty()) {
        urlString.append("?");
      }
      Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<String, String> oneParam = it.next();
        urlString
          .append(oneParam.getKey()).append("=")
          .append(URLEncoder.encode(oneParam.getValue(), "UTF-8"));
        if (it.hasNext()) {
          urlString.append("&");
        }
      }
    }
    
    // init connection
    URL url = new URL(urlString.toString());
    HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
    urlConn.setRequestMethod("POST");
    urlConn.setDoInput(true);
    if (null != body) {
      urlConn.setDoOutput(true);
    } 
    else {
      urlConn.setDoOutput(false);
    }
    urlConn.setUseCaches(false);
    if (null == contentType) {
      contentType = "text/xml; charset=utf-8";
    }
    urlConn.setRequestProperty("Content-Type", contentType);

    // send POST output
    if (null != body) {
      OutputStreamWriter printout = new OutputStreamWriter(
        urlConn.getOutputStream(), "UTF8");
      printout.write(body);
      printout.flush();
      printout.close();
    }
    
    // get response data
    if (null == responseEncoding) {
      responseEncoding = Charset.defaultCharset().toString();
    }
    return IOUtils.toString(
      urlConn.getInputStream(), responseEncoding);
  }

  public String getMetadataText(MGraph model, NonLiteral subj) {
    String text = "";
    for (Iterator<Triple> it = model.filter(subj, Properties.NIE_PLAINTEXTCONTENT, null); it.hasNext();) {
      text += getLexicalForm(it.next().getObject());
    }
    if (text.trim().length() > 0) {
      return text;
    }
    return null;
  }
  public String getMetadataLanguage(MGraph model, NonLiteral subj) {
    Iterator<Triple> it = model.filter(subj, Properties.DC_LANGUAGE, null);
    if (it.hasNext()) {
      Resource langNode = it.next().getObject();
      return getLexicalForm(langNode);
    }
    return null;
  }
  
  public String getLexicalForm(Resource res) {
    if (res == null) {
      return null;
    }
    else if (res instanceof Literal) {
      return ((Literal)res).getLexicalForm();
    }
    else
      return res.toString();
  }
  
  /**
   * The activate method.
   * 
   * @param ce
   *            the {@link ComponentContext}
   */
  protected void activate(@SuppressWarnings("unused") ComponentContext ce) {
    if (ce != null) {
    	this.bundleContext = ce.getBundleContext();
      //TODO initialize Extractor
      Dictionary<String,String> properties = ce.getProperties();
      String license = properties.get(LICENSE_KEY);
      String url = properties.get(CALAIS_URL_KEY);
      setLicenseKey(license);
      setCalaisUrl(url);
//      this.tcManager = TcManager.getInstance();
    }
  }
  
  /**
   * The deactivate method.
   * 
   * @param ce
   *            the {@link ComponentContext}
   */
  protected void deactivate(@SuppressWarnings("unused") ComponentContext ce) {

  }


}
