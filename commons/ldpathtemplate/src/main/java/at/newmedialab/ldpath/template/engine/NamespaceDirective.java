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

import at.newmedialab.ldpath.template.model.freemarker.TemplateWrapperModel;
import freemarker.core.Environment;
import freemarker.template.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Add file description here!
 * <p/>
 * Author: Sebastian Schaffert
 */
public class NamespaceDirective implements TemplateDirectiveModel {


    public NamespaceDirective() {
    }

    /**
     * Executes this user-defined directive; called by FreeMarker when the user-defined
     * directive is called in the template.
     *
     * @param env      the current processing environment. Note that you can access
     *                 the output {@link java.io.Writer Writer} by {@link freemarker.core.Environment#getOut()}.
     * @param params   the parameters (if any) passed to the directive as a
     *                 map of key/value pairs where the keys are {@link String}-s and the
     *                 values are {@link freemarker.template.TemplateModel} instances. This is never
     *                 <code>null</code>. If you need to convert the template models to POJOs,
     *                 you can use the utility methods in the {@link freemarker.template.utility.DeepUnwrap} class.
     * @param loopVars an array that corresponds to the "loop variables", in
     *                 the order as they appear in the directive call. ("Loop variables" are out-parameters
     *                 that are available to the nested body of the directive; see in the Manual.)
     *                 You set the loop variables by writing this array. The length of the array gives the
     *                 number of loop-variables that the caller has specified.
     *                 Never <code>null</code>, but can be a zero-length array.
     * @param body     an object that can be used to render the nested content (body) of
     *                 the directive call. If the directive call has no nested content (i.e., it is like
     *                 [@myDirective /] or [@myDirective][/@myDirective]), then this will be
     *                 <code>null</code>.
     * @throws freemarker.template.TemplateException
     *
     * @throws java.io.IOException
     */
    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
        TemplateWrapperModel<Map<String,String>> namespacesWrapped = (TemplateWrapperModel<Map<String,String>>)env.getGlobalVariable("namespaces");

        Map<String,String> namespaces;
        if(namespacesWrapped == null) {
            namespaces = new HashMap<String, String>();
            namespacesWrapped = new TemplateWrapperModel<Map<String, String>>(namespaces);
            env.setGlobalVariable("namespaces",namespacesWrapped);
        } else {
            namespaces = namespacesWrapped.getAdaptedObject(Map.class);
        }


        Iterator paramIter = params.entrySet().iterator();
        while (paramIter.hasNext()) {
            Map.Entry ent = (Map.Entry) paramIter.next();

            String paramName = (String) ent.getKey();
            TemplateModel paramValue = (TemplateModel) ent.getValue();

            if(paramValue instanceof TemplateScalarModel) {
                String uri = ((TemplateScalarModel)paramValue).getAsString();

                try {
                    URI test = new URI(uri);
                    namespaces.put(paramName,test.toString());
                } catch (URISyntaxException e) {
                    throw new TemplateModelException("invalid namespace URI '"+uri+"'",e);
                }
            }
        }

    }
}
