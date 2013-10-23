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
import at.newmedialab.ldpath.model.transformers.DateTimeTransformer;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

import java.util.Date;

/**
 * Transform a node into the freemarker date type ({@link freemarker.template.TemplateDateModel}).
 * <p/>
 * Author: Sebastian Schaffert
 */
public class TemplateDateTransformer<Node> implements NodeTransformer<TemplateDateModel,Node> {

    private DateTimeTransformer<Node> delegate;

    private int type = TemplateDateModel.UNKNOWN;

    public TemplateDateTransformer() {
        delegate = new DateTimeTransformer<Node>();
    }

    public TemplateDateTransformer(int type) {
        this.type = type;
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
    public TemplateDateModel transform(final RDFBackend<Node> nodeRDFBackend, final Node node) throws IllegalArgumentException {
        return new TemplateDateModel() {
            @Override
            public Date getAsDate() throws TemplateModelException {
                return delegate.transform(nodeRDFBackend, node);
            }

            @Override
            public int getDateType() {
                return type;
            }
        };
    }
}
