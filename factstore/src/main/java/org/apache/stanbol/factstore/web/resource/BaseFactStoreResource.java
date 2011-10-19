package org.apache.stanbol.factstore.web.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.factstore.web.FactStoreWebFragment;

public class BaseFactStoreResource extends BaseStanbolResource {

    @Override
    @SuppressWarnings("unchecked")
    public List<ScriptResource> getRegisteredScriptResources() {
        if (servletContext != null) {
            List<ScriptResource> scriptResources = (List<ScriptResource>) servletContext
                    .getAttribute(SCRIPT_RESOURCES);

            List<ScriptResource> fragmentsScriptResources = new ArrayList<ScriptResource>();
            for (ScriptResource scriptResource : scriptResources) {
                if (scriptResource.getFragmentName().equals("home") ||
                    scriptResource.getFragmentName().equals(FactStoreWebFragment.NAME)) {
                    fragmentsScriptResources.add(scriptResource);
                }
            }
            return fragmentsScriptResources;
        } else {
            return Collections.emptyList();
        }
    }
    
}
