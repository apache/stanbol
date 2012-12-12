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

package at.newmedialab.ldpath.template.model.transformers;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.api.transformers.NodeTransformer;
import at.newmedialab.ldpath.model.transformers.FloatTransformer;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

/**
 * Transform a node into the freemarker float type (TemplateFloatModel).
 * <p/>
 * Author: Sebastian Schaffert
 */
public class TemplateFloatTransformer<Node> implements NodeTransformer<TemplateNumberModel,Node> {

    private FloatTransformer<Node> delegate;

    public TemplateFloatTransformer() {
        delegate = new FloatTransformer<Node>();
    }

    /**
     * Transform the KiWiNode node into the datatype T. In case the node cannot be transformed to
     * the respective datatype, throws an IllegalArgumentException that needs to be caught by the class
     * carrying out the transformation.
     *
     * @param node
     * @return
     */
    @Override
    public TemplateNumberModel transform(final RDFBackend<Node> nodeRDFBackend, final Node node) throws IllegalArgumentException {
        return new TemplateNumberModel() {
            @Override
            public Number getAsNumber() throws TemplateModelException {
                return delegate.transform(nodeRDFBackend,node);
            }
        };
    }
}
