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

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.exception.LDPathParseException;
import at.newmedialab.ldpath.model.Constants;
import at.newmedialab.ldpath.template.model.freemarker.TemplateNodeModel;
import at.newmedialab.ldpath.template.model.freemarker.TemplateStackModel;
import at.newmedialab.ldpath.template.model.freemarker.TemplateWrapperModel;
import at.newmedialab.ldpath.template.model.transformers.*;
import at.newmedialab.ldpath.template.*;
import at.newmedialab.ldpath.template.util.FormatUtil;
import freemarker.core.Environment;
import freemarker.template.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A directive for inserting values retrieved with LDPath. It can be used in several forms:
 * <p/>
 * <strong>Value Insertion (without body):</strong><br/>
 * Allows inserting the value of a path expression. The path expression should specify an XML schema type for the
 * type conversion. If no type is given, string conversion is assumed. If the expression would return several values,
 * only the first result is taken.
 * <p/>
 * <code>
 * &lt;@ldpath path="... :: xsd:type">
 * </code>
 * <p/>
 * <strong>Value Iteration (with body):</strong>
 * Allows iterating over result nodes of a path expression. Further path expressions can be used to select the
 * values of each result node.
 * <p/>
 * <code>
 * &lt;@ldpath path="..."><br/>
 *    &nbsp;...<br/>
 *    &nbsp;&lt;@ldpath path="..."><br/>
 *    &nbsp;...<br/>
 * &lt;/@ldpath>
 * </code>
 * <p/>
 * If a loop variable is given, it will be bound to the context node. The context node is also implicitly available
 * as the variable "context".
 * <p/>
 * Author: Sebastian Schaffert
 */
public class LDPathDirective<Node> implements TemplateDirectiveModel {


    private LDPath<Node>     ldpath;
    private RDFBackend<Node> backend;

    public LDPathDirective(RDFBackend<Node> backend) {
        this.ldpath  = new LDPath<Node>(backend);
        this.backend = backend;

        // register custom freemarker transformers for the parser so we get the results immediately in the freemarker model
        ldpath.registerTransformer(Constants.NS_XSD + "string", new TemplateScalarTransformer<Node>());
        ldpath.registerTransformer(Constants.NS_XSD + "decimal", new TemplateLongTransformer<Node>());
        ldpath.registerTransformer(Constants.NS_XSD + "integer", new TemplateIntegerTransformer<Node>());
        ldpath.registerTransformer(Constants.NS_XSD + "long", new TemplateLongTransformer<Node>());
        ldpath.registerTransformer(Constants.NS_XSD + "short", new TemplateIntegerTransformer<Node>());
        ldpath.registerTransformer(Constants.NS_XSD + "double", new TemplateDoubleTransformer<Node>());
        ldpath.registerTransformer(Constants.NS_XSD + "float", new TemplateFloatTransformer<Node>());
        ldpath.registerTransformer(Constants.NS_XSD + "dateTime", new TemplateDateTransformer<Node>(TemplateDateModel.DATETIME));
        ldpath.registerTransformer(Constants.NS_XSD + "date", new TemplateDateTransformer<Node>(TemplateDateModel.DATE));
        ldpath.registerTransformer(Constants.NS_XSD + "time", new TemplateDateTransformer<Node>(TemplateDateModel.TIME));
        ldpath.registerTransformer(Constants.NS_XSD + "boolean", new TemplateBooleanTransformer<Node>());
        ldpath.registerTransformer(Constants.NS_XSD + "anyURI", new TemplateScalarTransformer<Node>());

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
        TemplateStackModel contextStack = (TemplateStackModel)env.getVariable("context");
        if(contextStack == null || contextStack.empty()) {
            throw new TemplateModelException("error; no context node available");
        }
        TemplateNodeModel<Node> context = (TemplateNodeModel<Node>)contextStack.peek();

        SimpleScalar pathScalar = (SimpleScalar)params.get("path");
        if(pathScalar == null) {
            throw new TemplateException("the directive has been called without a path parameter",env);
        }
        String path = pathScalar.getAsString();

        TemplateWrapperModel<Map<String,String>> namespacesWrapped = (TemplateWrapperModel<Map<String,String>>)env.getGlobalVariable("namespaces");

        Map<String,String> namespaces;
        if(namespacesWrapped == null) {
            namespaces = new HashMap<String, String>();
            namespacesWrapped = new TemplateWrapperModel<Map<String, String>>(new HashMap<String, String>());
            env.setGlobalVariable("namespaces",namespacesWrapped);
        } else {
            namespaces = namespacesWrapped.getAdaptedObject(Map.class);
        }


        if(body == null) { // value insertion
            if(!path.contains("::")) {
                path = path + ":: xsd:string";
            }
            try {
                Collection results = ldpath.pathTransform(context.getNode(),path,namespaces);

                if(results.size() > 0) {
                    Object result = results.iterator().next();

                    if(result instanceof TemplateNumberModel) {
                        env.getOut().write(FormatUtil.formatNumber( ((TemplateNumberModel)result).getAsNumber() ));
                    } else if(result instanceof TemplateDateModel) {
                        switch (((TemplateDateModel)result).getDateType()) {
                            case TemplateDateModel.DATE:
                                env.getOut().write(FormatUtil.formatDate(((TemplateDateModel)result).getAsDate()));
                                break;
                            case TemplateDateModel.TIME:
                                env.getOut().write(FormatUtil.formatTime(((TemplateDateModel)result).getAsDate()));
                                break;
                            case TemplateDateModel.DATETIME:
                                env.getOut().write(FormatUtil.formatDateTime(((TemplateDateModel)result).getAsDate()));
                                break;
                            default:
                                env.getOut().write(FormatUtil.formatDateTime(((TemplateDateModel)result).getAsDate()));
                        }
                    } else if(result instanceof TemplateScalarModel) {
                        env.getOut().write( ((TemplateScalarModel)result).getAsString() );
                    } else if(result instanceof TemplateBooleanModel) {
                        env.getOut().write( Boolean.toString(((TemplateBooleanModel)result).getAsBoolean()) );
                    }

                } // else write nothing


            } catch (LDPathParseException e) {
                throw new TemplateException("invalid path for ldpath directive: "+path,e,env);
            }

        } else {
            try {
                for(Node node : ldpath.pathQuery(context.getNode(),path,namespaces)) {
                    contextStack.push(new TemplateNodeModel<Node>(node, backend));

                    if(loopVars.length > 0) {
                        loopVars[0] = new TemplateNodeModel<Node>(node,backend);
                    }

                    body.render(env.getOut());

                    contextStack.pop();
                }
            } catch(LDPathParseException ex) {
                throw new TemplateException("invalid path for ldpath directive: "+path,ex,env);
            }

        }

    }
}
