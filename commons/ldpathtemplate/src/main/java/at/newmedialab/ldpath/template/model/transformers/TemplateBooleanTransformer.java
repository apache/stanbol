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
import at.newmedialab.ldpath.model.transformers.BooleanTransformer;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;

/**
 * Transform a node into the freemarker boolean type (TemplateBooleanModel).
 * <p/>
 * Author: Sebastian Schaffert
 */
public class TemplateBooleanTransformer<Node> implements NodeTransformer<TemplateBooleanModel,Node> {

    private BooleanTransformer<Node> delegate;

    public TemplateBooleanTransformer() {
        delegate = new BooleanTransformer<Node>();
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
    public TemplateBooleanModel transform(final RDFBackend<Node> nodeRDFBackend, final Node node) throws IllegalArgumentException {
        return new TemplateBooleanModel() {
            @Override
            public boolean getAsBoolean() throws TemplateModelException {
                return delegate.transform(nodeRDFBackend,node);
            }
        };
    }
}
