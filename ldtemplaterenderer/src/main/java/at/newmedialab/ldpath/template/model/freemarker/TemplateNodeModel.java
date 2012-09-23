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

package at.newmedialab.ldpath.template.model.freemarker;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import freemarker.template.*;

/**
 * A custom freemarker model to represent RDF nodes (implementation independent, generic)
 * <p/>
 * Author: Sebastian Schaffert
 */
public class TemplateNodeModel<Node> implements TemplateModel, TemplateHashModel, AdapterTemplateModel {

    private Node node;
    private RDFBackend<Node> backend;

    public TemplateNodeModel(Node node, RDFBackend<Node> backend) {
        this.node    = node;
        this.backend = backend;
    }

    public Node getNode() {
        return node;
    }

    /**
     * Gets a <tt>TemplateModel</tt> from the hash.
     *
     * @param key the name by which the <tt>TemplateModel</tt>
     *            is identified in the template.
     * @return the <tt>TemplateModel</tt> referred to by the key,
     *         or null if not found.
     */
    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        if( ("uri".equals(key) && backend.isURI(node)) || ("content".equals(key) && backend.isLiteral(node))) {
            return new TemplateScalarModel() {
                @Override
                public String getAsString() throws TemplateModelException {
                    return backend.stringValue(node);
                }
            };
        } else if("language".equals(key) && backend.isLiteral(node)) {
            return new TemplateScalarModel() {
                @Override
                public String getAsString() throws TemplateModelException {
                    return backend.getLiteralLanguage(node).getLanguage();
                }
            };
        } else if("type".equals(key) && backend.isLiteral(node)) {
            return new TemplateScalarModel() {
                @Override
                public String getAsString() throws TemplateModelException {
                    return backend.getLiteralType(node).toString();
                }
            };
        } else if("id".equals(key) && backend.isBlank(node)) {
            return new TemplateScalarModel() {
                @Override
                public String getAsString() throws TemplateModelException {
                    return backend.stringValue(node);
                }
            };
        }
        return null;
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    /**
     * Retrieves the underlying object, or some other object semantically
     * equivalent to its value narrowed by the class hint.
     *
     * @param hint the desired class of the returned value. An implementation
     *             should make reasonable effort to retrieve an object of the requested
     *             class, but if that is impossible, it must at least return the underlying
     *             object as-is. As a minimal requirement, an implementation must always
     *             return the exact underlying object when
     *             <tt>hint.isInstance(underlyingObject) == true</tt> holds. When called
     *             with <tt>java.lang.Object.class</tt>, it should return a generic Java
     *             object (i.e. if the model is wrapping a scripting lanugage object that is
     *             further wrapping a Java object, the deepest underlying Java object should
     *             be returned).
     * @return the underlying object, or its value accommodated for the hint
     *         class.
     */
    @Override
    public Object getAdaptedObject(Class hint) {
        return node;
    }
}
