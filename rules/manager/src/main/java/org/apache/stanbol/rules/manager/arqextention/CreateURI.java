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
package org.apache.stanbol.rules.manager.arqextention;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.Binding1;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArgType;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionEval;
import com.hp.hpl.jena.sparql.util.IterLib;

public class CreateURI extends PropertyFunctionEval {

    public CreateURI() {
        super(PropFuncArgType.PF_ARG_SINGLE, PropFuncArgType.PF_ARG_SINGLE);
    }

    @Override
    public QueryIterator execEvaluated(Binding binding,
                                       PropFuncArg argumentSubject,
                                       Node predicate,
                                       PropFuncArg argumentObject,
                                       ExecutionContext execCxt) {

        Binding b = null;
        if (argumentObject.getArg().isLiteral()) {
            Node ref = argumentSubject.getArg();
            if (ref.isVariable()) {
                String argumentString = argumentObject.getArg().toString().replace("\"", "");
                //STANBOL-621: Binding1 has no longer a public constructor
                //b = new Binding1(binding, Var.alloc(ref), NodeFactory.createURI(argumentString));
                b = BindingFactory.binding(binding, Var.alloc(ref), NodeFactory.createURI(argumentString));
            }
        }

        if (b == null) {
            b = binding;
        }

        return IterLib.result(b, execCxt);
    }

}
