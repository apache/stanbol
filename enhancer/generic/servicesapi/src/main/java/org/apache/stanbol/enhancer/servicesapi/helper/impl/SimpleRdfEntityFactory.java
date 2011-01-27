package org.apache.stanbol.enhancer.servicesapi.helper.impl;

import java.lang.reflect.Proxy;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.stanbol.enhancer.servicesapi.helper.RdfEntity;
import org.apache.stanbol.enhancer.servicesapi.helper.RdfEntityFactory;


public class SimpleRdfEntityFactory extends RdfEntityFactory {

    private final MGraph graph;
    private final LiteralFactory literalFactory;

    public SimpleRdfEntityFactory(MGraph graph) {
        if (graph == null){
            throw new IllegalArgumentException("The MGraph parsed as parameter MUST NOT be NULL!");
        }
        this.graph = graph;
        literalFactory = LiteralFactory.getInstance();
    }

    @SuppressWarnings("unchecked")
    public <T extends RdfEntity> T getProxy(NonLiteral rdfNode, Class<T> type,Class<?>...additionalInterfaces) {
        Class<?>[] interfaces = new Class<?>[additionalInterfaces.length+1];
        interfaces[0] = type;
        System.arraycopy(additionalInterfaces, 0, interfaces, 1, additionalInterfaces.length);
        //Class<?> proxy = Proxy.getProxyClass(WrapperFactory.class.getClassLoader(), interfaces);
        Object instance = Proxy.newProxyInstance(
                SimpleRdfEntityFactory.class.getClassLoader(),
                interfaces,
                new RdfProxyInvocationHandler(this, rdfNode, interfaces, literalFactory));
        return (T)instance;
    }

    protected MGraph getGraph() {
        return graph;
    }

}
