package org.apache.stanbol.entityhub.ldpath.transformer;

import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory.ValueConverter;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.api.transformers.NodeTransformer;

/**
 * LDPath {@link NodeTransformer} internally using the Entityhub 
 * {@link ValueConverter}. <p>
 * This transformer should be used for plain literals and references (xsd:anyURI)
 * to ensure that nodes are transformed to {@link Text} and {@link Reference}
 * instances.<p>
 * Users should use {@link LDPathUtils#createAndInitLDPath(RDFBackend, ValueFactory)}
 * to ensure that {@link LDPath} instances are configured accordingly.
 *  
 * @author Rupert Westenthaler.
 * @see LDPathUtils#createAndInitLDPath(RDFBackend, ValueFactory)
 * @param <T>
 */
public class ValueConverterTransformerAdapter<T> implements NodeTransformer<T,Object> {

    private final ValueFactory vf;
    private final ValueConverter<T> vc;
    
    public ValueConverterTransformerAdapter(ValueConverter<T> vc, ValueFactory vf){
        this.vf = vf == null ? InMemoryValueFactory.getInstance() : vf;
        this.vc = vc;
    }
    @Override
    public T transform(RDFBackend<Object> backend, Object node) throws IllegalArgumentException {
        T value = vc.convert(node, vf);
        if(value == null){
            value = vc.convert(backend.stringValue(node), vf);
        }
        if(value == null){
            throw new IllegalArgumentException("Unable to transform node '"+
                node+"' to data type '"+vc.getDataType()+"'!");
        } else {
            return value;
        }
    }

}
