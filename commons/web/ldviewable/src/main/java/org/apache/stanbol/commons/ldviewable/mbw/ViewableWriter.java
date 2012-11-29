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
package org.apache.stanbol.commons.ldviewable.mbw;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.ldpathtemplate.LdRenderer;
import org.apache.stanbol.commons.ldviewable.Viewable;

@Component
@Service(ViewableWriter.class)
@Produces("text/html")
public class ViewableWriter implements MessageBodyWriter<Viewable> {

	@Reference
	private LdRenderer ldRenderer;
	
	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return Viewable.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(Viewable t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(final Viewable t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		Writer out = new OutputStreamWriter(entityStream, "utf-8"); 
		ldRenderer.renderPojo(new Wrapper(t.getPojo()), "html/"+t.getTemplatePath(), out);
		out.flush();
	}
	static public class Wrapper {

		private Object wrapped;
		public Wrapper(Object wrapped) {
			this.wrapped = wrapped;
		}
		public Object getIt() {
			return wrapped;
		}
		
		
	}

}


//TODO check if some frremarker config settings should be taken from there
//public class FreemarkerViewProcessor implements ViewProcessor<Template> {
//
//    private final Logger log = LoggerFactory.getLogger(getClass());
//
//    protected Configuration freemarkerConfig;
//
//    protected TemplateLoader templateLoader;
//
//    @Context
//    protected ServletContext context;
//
//    public FreemarkerViewProcessor(TemplateLoader templateLoader) {
//        this.templateLoader = templateLoader;
//    }
//
//    /**
//     * @return extension for templates, ".ftl" by default; if we don't see this
//     *         at the end of your view we'll append it so we can find the
//     *         template resource
//     */
//    protected String getDefaultExtension() {
//        return ".ftl";
//    }
//
//    /**
//     * Define additional variables to make available to the template.
//     *
//     * @param viewableVariables variables provided by whomever generated the
//     *            viewable object; these are provided for reference only, there
//     *            will be no effect if you modify this map
//     * @return new variables for the template context, which will override any
//     *         defaults provided
//     */
//    protected Map<String, Object> getVariablesForTemplate(
//            final Map<String, Object> viewableVariables) {
//        return Collections.emptyMap();
//    }
//
//    /**
//     * Catch any exception generated during template processing.
//     *
//     * @param t throwable caught
//     * @param templatePath path of template we're executing
//     * @param templateContext context use when evaluating this template
//     * @param out output stream from servlet container
//     * @throws IOException on any write errors, or if you want to rethrow
//     */
//    protected void onProcessException(final Throwable t,
//            final Template template, final Map<String, Object> templateContext,
//            final OutputStream out) throws IOException {
//        log.error("Error processing freemarker template @ "
//                + template.getName() + ": " + t.getMessage(), t);
//        out.write("<pre>".getBytes());
//        t.printStackTrace(new PrintStream(out));
//        out.write("</pre>".getBytes());
//    }
//
//    /**
//     * Modify freemarker configuration after we've created it and applied any
//     * settings from 'freemarker.properties' on the classpath.
//     *
//     * @param config configuration we've created so far
//     * @param context servlet context used to create the configuration
//     */
//    protected void assignFreemarkerConfig(final Configuration config,
//            final ServletContext context) {
//        // TODO read those parameters from context instead of hardcoding them
//
//        // don't always put a ',' in numbers (e.g., id=2000 vs id=2,000)
//        config.setNumberFormat("0");
//
//        // don't look for list.en.ftl when list.ftl requested
//        config.setLocalizedLookup(false);
//
//        // don't cache for more that 2s
//        config.setTemplateUpdateDelay(2);
//        config.setDefaultEncoding("utf-8");
//        config.setOutputEncoding("utf-8");
//        log.info("Assigned default freemarker configuration");
//    }
//
//    protected Configuration getConfig() {
//        if (freemarkerConfig == null) {
//            // deferred initialization of the freemarker config to ensure that
//            // the injected ServletContext is fully functional
//            Configuration config = new Configuration();
//            config.setTemplateLoader(templateLoader);
//
//            // TODO: make the usage of a freemaker properties file an explicit
//            // parameter declared in the servlet context instead of magic
//            // classloading auto-detect. That way the application could
//            // explicitly override the defaults
//            final InputStream fmProps = context.getResourceAsStream("freemarker.properties");
//            boolean loadDefaults = true;
//            if (fmProps != null) {
//                try {
//                    config.setSettings(fmProps);
//                    log.info("Assigned freemarker configuration from 'freemarker.properties'");
//                    loadDefaults = false;
//                } catch (Throwable t) {
//                    log.warn(
//                            "Failed to load/assign freemarker.properties, will"
//                                    + " use default settings instead: "
//                                    + t.getMessage(), t);
//                }
//            }
//            if (loadDefaults) {
//                assignFreemarkerConfig(config, context);
//            }
//            freemarkerConfig = config;
//        }
//        return freemarkerConfig;
//    }
//
//    public Template resolve(final String path) {
//        // accept both '/path/to/template' and '/path/to/template.ftl'
//        final String defaultExtension = getDefaultExtension();
//        final String filePath = path.endsWith(defaultExtension) ? path : path
//                + defaultExtension;
//        try {
//            return getConfig().getTemplate(filePath);
//        } catch (IOException e) {
//            log.error("Failed to load freemaker template: " + filePath);
//            return null;
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    public void writeTo(Template template, Viewable viewable, OutputStream out)
//            throws IOException {
//        out.flush(); // send status + headers
//
//        Object model = viewable.getModel();
//        final Map<String, Object> vars = new HashMap<String, Object>();
//        if (model instanceof Map<?, ?>) {
//            vars.putAll((Map<String, Object>) model);
//        } else {
//            vars.put("it", model);
//        }
//        // override custom variables if any
//        vars.putAll(getVariablesForTemplate(vars));
//
//        final OutputStreamWriter writer = new OutputStreamWriter(out,"utf-8");
//        try {
//            template.process(vars, writer);
//        } catch (Throwable t) {
//            onProcessException(t, template, vars, out);
//        }
//    }
//
//}

