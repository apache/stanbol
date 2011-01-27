package org.apache.stanbol.enhancer.engines.autotagging.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.autotagging.Autotagger;
import org.apache.stanbol.autotagging.TagInfo;
import org.apache.stanbol.enhancer.engines.autotagging.AutotaggerProvider;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Main Stanbol Enhancer standalone servlet, registers with the OSGi HttpService to process
 * requests on /enhancer.
 */
@Component(immediate = true)
@Service
@SuppressWarnings("serial")
public class AutotaggingServlet extends HttpServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String ALIAS = "/suggest";

    @Reference
    HttpService httpService;

    @Reference
    AutotaggerProvider provider;


    @Override
    /** Create a ContentItem and queue for enhancement */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req,resp);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String labelParam = getEntityName(req);
        String typeParam = getEntityType(req);
        Autotagger autotagger = provider.getAutotagger();
        List<TagInfo> suggestions;
        if(typeParam == null){
            suggestions = autotagger.suggest(labelParam);
        } else {
            suggestions = autotagger.suggestForType(labelParam, typeParam);
        }
        //encode the results and sent it back to the client
        resp.setContentType("text/json");
        resp.setCharacterEncoding("UTF-8");
        Writer writer = resp.getWriter();
        JSONObject suggestionList = new JSONObject();
        List<Map<String, Object>> suggestionObjects = new ArrayList<Map<String,Object>>(suggestions.size());
        for (TagInfo suggestion : suggestions){
            Map<String,Object> map = new HashMap<String, Object>();
            map.put("uri", suggestion.getId());
            map.put("label",suggestion.getLabel());
            map.put("type",suggestion.getType());
            map.put("confidence", suggestion.getConfidence());
            suggestionObjects.add(map);
        }
        try {
            suggestionList.put("suggestion", suggestionObjects);
        } catch (JSONException e) {
            log.error("Unable to encode suggestions as JSON",e);
            resp.sendError(500, e.getMessage());
            return;
        }
        writer.append(suggestionList.toString());
    }

    private String getEntityName(HttpServletRequest r) {
        final String result = r.getParameter("name");
        if (result == null || result.length() == 0) {
            throw new IllegalArgumentException(
                    "Missing Parameter name, request should include parameter \"name\"");
        }
        return result;
    }

    private String getEntityType(HttpServletRequest r) {
        final String result = r.getParameter("type");
//        if(result == null){
//            return null;
//        } else {
//            //convert the Type to the Ontology
//        }
        return result;
    }
    @Activate
    protected void activate(ComponentContext ctx) throws Exception {
        httpService.registerServlet(ALIAS, this, null, null);
        log.info("Servlet registered at {}", ALIAS);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) throws Exception {
        httpService.unregister(ALIAS);
        log.info("Servlet unregistered from {}", ALIAS);
    }
}
