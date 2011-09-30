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
package org.apache.stanbol.commons.web.base.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.spi.resource.Singleton;
import com.sun.jersey.spi.template.ViewProcessor;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Match a Viewable-named view with a Freemarker template.
 *
 * This class is based on the following original implementation:
 * http://github.com/cwinters/jersey-freemarker/
 *
 * <p>
 * You can configure the location of your templates with the context param
 * 'freemarker.template.path'. If not assigned we'll use a default of
 * <tt>WEB-INF/templates</tt>. Note that this uses Freemarker's
 * {@link freemarker.cache.WebappTemplateLoader} to load/cache the templates, so
 * check its docs (or crank up the logging under the 'freemarker.cache' package)
 * if your templates aren't getting loaded.
 * </p>
 *
 * <p>
 * This will put your Viewable's model object in the template variable "it",
 * unless the model is a Map. If so, the values will be assigned to the template
 * assuming the map is of type <tt>Map&lt;String,Object></tt>.
 * </p>
 *
 * <p>
 * There are a number of methods you can override to change the behavior, such
 * as handling processing exceptions, changing the default template extension,
 * or adding variables to be assigned to every template context.
 * </p>
 *
 * @author Chris Winters <chris@cwinters.com> // original code
 * @author Olivier Grisel <ogrisel@nuxeo.com> // ViewProcessor refactoring
 */
@Singleton
@Provider
public class FreemarkerViewProcessor implements ViewProcessor<Template> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Configuration freemarkerConfig;

    protected TemplateLoader templateLoader;

    @Context
    protected ServletContext context;

    public FreemarkerViewProcessor(TemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }

    /**
     * @return extension for templates, ".ftl" by default; if we don't see this
     *         at the end of your view we'll append it so we can find the
     *         template resource
     */
    protected String getDefaultExtension() {
        return ".ftl";
    }

    /**
     * Define additional variables to make available to the template.
     *
     * @param viewableVariables variables provided by whomever generated the
     *            viewable object; these are provided for reference only, there
     *            will be no effect if you modify this map
     * @return new variables for the template context, which will override any
     *         defaults provided
     */
    protected Map<String, Object> getVariablesForTemplate(
            final Map<String, Object> viewableVariables) {
        return Collections.emptyMap();
    }

    /**
     * Catch any exception generated during template processing.
     *
     * @param t throwable caught
     * @param templatePath path of template we're executing
     * @param templateContext context use when evaluating this template
     * @param out output stream from servlet container
     * @throws IOException on any write errors, or if you want to rethrow
     */
    protected void onProcessException(final Throwable t,
            final Template template, final Map<String, Object> templateContext,
            final OutputStream out) throws IOException {
        log.error("Error processing freemarker template @ "
                + template.getName() + ": " + t.getMessage(), t);
        out.write("<pre>".getBytes());
        t.printStackTrace(new PrintStream(out));
        out.write("</pre>".getBytes());
    }

    /**
     * Modify freemarker configuration after we've created it and applied any
     * settings from 'freemarker.properties' on the classpath.
     *
     * @param config configuration we've created so far
     * @param context servlet context used to create the configuration
     */
    protected void assignFreemarkerConfig(final Configuration config,
            final ServletContext context) {
        // TODO read those parameters from context instead of hardcoding them

        // don't always put a ',' in numbers (e.g., id=2000 vs id=2,000)
        config.setNumberFormat("0");

        // don't look for list.en.ftl when list.ftl requested
        config.setLocalizedLookup(false);

        // don't cache for more that 2s
        config.setTemplateUpdateDelay(2);
        config.setDefaultEncoding("utf-8");
        config.setOutputEncoding("utf-8");
        log.info("Assigned default freemarker configuration");
    }

    protected Configuration getConfig() {
        if (freemarkerConfig == null) {
            // deferred initialization of the freemarker config to ensure that
            // the injected ServletContext is fully functional
            Configuration config = new Configuration();
            config.setTemplateLoader(templateLoader);

            // TODO: make the usage of a freemaker properties file an explicit
            // parameter declared in the servlet context instead of magic
            // classloading auto-detect. That way the application could
            // explicitly override the defaults
            final InputStream fmProps = context.getResourceAsStream("freemarker.properties");
            boolean loadDefaults = true;
            if (fmProps != null) {
                try {
                    config.setSettings(fmProps);
                    log.info("Assigned freemarker configuration from 'freemarker.properties'");
                    loadDefaults = false;
                } catch (Throwable t) {
                    log.warn(
                            "Failed to load/assign freemarker.properties, will"
                                    + " use default settings instead: "
                                    + t.getMessage(), t);
                }
            }
            if (loadDefaults) {
                assignFreemarkerConfig(config, context);
            }
            freemarkerConfig = config;
        }
        return freemarkerConfig;
    }

    public Template resolve(final String path) {
        // accept both '/path/to/template' and '/path/to/template.ftl'
        final String defaultExtension = getDefaultExtension();
        final String filePath = path.endsWith(defaultExtension) ? path : path
                + defaultExtension;
        try {
            return getConfig().getTemplate(filePath);
        } catch (IOException e) {
            log.error("Failed to load freemaker template: " + filePath);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public void writeTo(Template template, Viewable viewable, OutputStream out)
            throws IOException {
        out.flush(); // send status + headers

        Object model = viewable.getModel();
        final Map<String, Object> vars = new HashMap<String, Object>();
        if (model instanceof Map<?, ?>) {
            vars.putAll((Map<String, Object>) model);
        } else {
            vars.put("it", model);
        }
        // override custom variables if any
        vars.putAll(getVariablesForTemplate(vars));

        final OutputStreamWriter writer = new OutputStreamWriter(out,"utf-8");
        try {
            template.process(vars, writer);
        } catch (Throwable t) {
            onProcessException(t, template, vars, out);
        }
    }

}
