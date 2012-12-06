package org.apache.stanbol.commons.namespaceprefix.provider.stanbol;

import java.util.Collections;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.impl.NamespacePrefixProviderImpl;
import org.apache.stanbol.commons.namespaceprefix.mappings.DefaultNamespaceMappingsEnum;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true, metatype=true)
@Service
@Property(name=Constants.SERVICE_RANKING,value="1000000")
public class DefaultNamespaceMappingsProvider extends NamespacePrefixProviderImpl implements NamespacePrefixProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultNamespaceMappingsProvider.class);

    public DefaultNamespaceMappingsProvider(){
        super(Collections.EMPTY_MAP);
        for(DefaultNamespaceMappingsEnum m : DefaultNamespaceMappingsEnum.values()){
            String current = addMapping(m.getPrefix(), m.getNamespace(), true);
            if(current != null){
                log.warn("Found duplicate mapping for prefix '{}'->[{},{}] in {}",
                    new Object[]{m.getPrefix(),current,m.getNamespace(),
                                 DefaultNamespaceMappingsEnum.class.getSimpleName()});
            }
        }        
    }

}
