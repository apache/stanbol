/*
 * Copyright (c) 2011 Salzburg Research.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.newmedialab.ldpath.template.engine;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.template.model.freemarker.TemplateNodeModel;
import at.newmedialab.ldpath.template.model.freemarker.TemplateStackModel;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Add file description here!
 * <p/>
 * Author: Sebastian Schaffert
 */
public class TemplateEngine<Node> {

    private Configuration freemarker;

    private RDFBackend<Node> backend;

    public TemplateEngine(RDFBackend<Node> backend) {

        this.backend = backend;

        freemarker = new Configuration();
        freemarker.setObjectWrapper(new DefaultObjectWrapper());


    }


    /**
     * Allow setting a different template loader. Custom template loaders can be implemented in addition to
     * those provided by FreeMarker.
     *
     * @param loader
     */
    public void setTemplateLoader(TemplateLoader loader) {
        freemarker.setTemplateLoader(loader);
    }


    public void setDirectoryForTemplateLoading(File dir) throws IOException {
        freemarker.setDirectoryForTemplateLoading(dir);
    }

    public void setServletContextForTemplateLoading(Object sctxt, String path) {
        freemarker.setServletContextForTemplateLoading(sctxt, path);
    }

    public void setClassForTemplateLoading(Class clazz, String pathPrefix) {
        freemarker.setClassForTemplateLoading(clazz, pathPrefix);
    }


    /**
     * Process the template with the given name forn the given context node and write the result to the given
     * output writer. The way the template is retrieved depends on the template loader, which can be set using the
     * setTemplateLoader() method.
     *
     * @param context the  initial context node to apply this template to
     * @param templateName the name of the template
     * @param out          where to write the results
     * @throws IOException
     * @throws TemplateException
     */
    public void processFileTemplate(Node context, String templateName, Writer out) throws IOException, TemplateException {
        processTemplate(context,freemarker.getTemplate(templateName),null,out);
    }

    /**
     * Process the template with the given name forn the given context node and write the result to the given
     * output writer. The initial environment is passed over to the invocation of the template. The way the template
     * is retrieved depends on the template loader, which can be set using the setTemplateLoader() method.
     *
     * @param context the  initial context node to apply this template to
     * @param templateName the name of the template
     * @param initialEnv   an initial root environment for processing the template
     * @param out          where to write the results
     * @throws IOException
     * @throws TemplateException
     */
    public void processFileTemplate(Node context, String templateName, Map initialEnv,  Writer out) throws IOException, TemplateException {
        processTemplate(context,freemarker.getTemplate(templateName),initialEnv,out);
    }


    public void processTemplate(Node context, Template template, Map initialEnv, Writer out) throws IOException, TemplateException {
        Map root = new HashMap();

        if(initialEnv != null) {
            for(Map.Entry entry : (Set<Map.Entry>) initialEnv.entrySet()) {
                root.put(entry.getKey(), entry.getValue());
            }
        }

        root.put("namespace", new NamespaceDirective());
        root.put("evalLDPath",new LDPathMethod(backend));
        root.put("ldpath",new LDPathDirective(backend));

        TemplateStackModel contexts = new TemplateStackModel();
        contexts.push(new TemplateNodeModel(context,backend));
        root.put("context",contexts);

        template.process(root,out);
    }
}
