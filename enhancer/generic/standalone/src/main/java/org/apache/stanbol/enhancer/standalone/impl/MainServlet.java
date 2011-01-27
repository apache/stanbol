package org.apache.stanbol.enhancer.standalone.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.clerezza.rdf.core.Triple;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Main Stanbol Enhancer standalone servlet, registers with the OSGi HttpService 
 * to process requests on /enhancer.
 */
@Component(immediate = true)
@Service
@SuppressWarnings("serial")
public class MainServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MainServlet.class);

    public static final String ALIAS = "/enhancer";

    @Reference
    HttpService httpService;

    @Reference
    EnhancementJobManager jobManager;

    @Reference
    Store store;

    @Override
    /** Create a ContentItem and queue for enhancement */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        final String id = getContentItemId(req);
        // TODO: avoid loading the complete stream in memory: use a disk based
        // buffer to avoid heap space saturation when uploading large content
        // such as videos
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(req.getInputStream(), bos);
        // TODO handle case where ContentItem already exists
        final ContentItem ci = store.create(id, bos.toByteArray(),
                req.getContentType());
        try {
            jobManager.enhanceContent(ci);
            store.put(ci);
        } catch (InvalidContentException e) {
            log.debug(e.toString(), e);
            // 400 Bad Request:
            // the client should fix the uploaded file / mimetype before
            // retrying
            resp.sendError(400, e.getMessage());
            return;
        } catch (EngineException e) {
            log.error(e.toString(), e);
            // 500 Internal Server Error:
            // The server encountered an unexpected condition which prevented it
            // from fulfilling the request.
            resp.sendError(500, e.getMessage());
            return;
        }
        log.info("Created {}, registered with EnhancementJobManager", ci);
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(ci.getId());
        resp.getWriter().write('\n');
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        final String id = getContentItemId(req);
        final ContentItem ci = store.get(id);
        if (ci == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, id
                    + " not found in store " + store);
            return;
        }
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        dumpContentItem(ci, resp.getWriter());
        resp.getWriter().write('\n');
    }

    private static void dumpContentItem(ContentItem ci, PrintWriter w) {
        w.print("**ContentItem:");
        w.println(ci.getId());
        w.println("**Metadata:");
        for (Triple o : ci.getMetadata().getGraph()) {
            w.println(o);
        }
    }

    private static String getContentItemId(HttpServletRequest r) {
        final String result = r.getPathInfo();
        if (result == null || result.length() == 0) {
            throw new IllegalArgumentException(
                    "Missing ContentItem id, request path should include id");
        }
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
