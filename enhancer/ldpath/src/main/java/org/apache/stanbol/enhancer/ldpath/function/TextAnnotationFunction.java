package org.apache.stanbol.enhancer.ldpath.function;

import static org.apache.stanbol.enhancer.ldpath.utils.Utils.parseSelector;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_EXTRACTED_FROM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.api.functions.SelectorFunction;
import at.newmedialab.ldpath.api.selectors.NodeSelector;
import at.newmedialab.ldpath.model.selectors.PropertySelector;
import at.newmedialab.ldpath.parser.ParseException;

public class TextAnnotationFunction implements SelectorFunction<Resource> {
    
    private final Logger log = LoggerFactory.getLogger(TextAnnotationFunction.class);

    private static final String FUNCTION_NAME = "textAnnotation";
    private static NodeSelector<Resource> selector;
    static {
        String path = String.format("^%s[%s is %s]",
            ENHANCER_EXTRACTED_FROM,RDF_TYPE,ENHANCER_TEXTANNOTATION);
        try {
            selector = parseSelector(path);
        } catch (ParseException e) {
            throw new IllegalStateException("Unable to parse the ld-path selector '" +
            		path + "'used by the 'fn:" + FUNCTION_NAME + "'!", e);
        }
    }
    private static NodeSelector<Resource> dcTypeSelector = new PropertySelector<Resource>(DC_TYPE);
    
    public TextAnnotationFunction() {
    }

    @Override
    public Collection<Resource> apply(RDFBackend<Resource> backend, Collection<Resource>... args) {
        if(args == null || args.length < 1 || args[0] == null || args[0].isEmpty()){
            throw new IllegalArgumentException("The 'fn:"+FUNCTION_NAME+"' function " +
            		"requires at least a single none empty parameter (the context). Use 'fn:" +
                    FUNCTION_NAME+"(.)' to execute it on the path context!");
        }
        Set<Resource> textAnnotations = new HashSet<Resource>();
        for(Resource context : args[0]){
            textAnnotations.addAll(selector.select(backend, context));
        }
// NOTE: parsing of the dc:type as parameter is deactivated for now, because
//       See the NOTES within this commented seciton for details why.
//        final UriRef dcTypeConstraint;
//        if(args.length < 2 || args[1].isEmpty()){
//            dcTypeConstraint = null;
//        } else {
//            /*
//             * NOTES:
//             * 
//             *  * Parameters MUST BE parsed as Literals, because otherwise LDPATH
//             *    would execute them rather than directly parsing them
//             *  * Namespace prefixes can not be supported for URIs parsed as
//             *    Literals, because the prefix mappings are only known by the
//             *    ldpath parser and not available to this component.
//             */
//            Resource value = args[1].iterator().next();
//            if(value instanceof Literal){
//                dcTypeConstraint = new UriRef(((Literal)value).getLexicalForm());
//            } else {
//                log.warn("Unable to use dc:type constraint {} (value MUST BE a Literal)!",value);
//                dcTypeConstraint = null;
//            }
//        }
//        if(dcTypeConstraint != null){
//            NodeTest<Resource> dcTypeFilter = new PathEqualityTest<Resource>(dcTypeSelector, dcTypeConstraint);
//            Iterator<Resource> it = textAnnotations.iterator();
//            while(it.hasNext()){
//                if(!dcTypeFilter.apply(backend, Collections.singleton(it.next()))){
//                    it.remove();
//                }
//            }
//        }
        return textAnnotations;
    }

    @Override
    public String getPathExpression(RDFBackend<Resource> backend) {
        return FUNCTION_NAME;
    }
}
