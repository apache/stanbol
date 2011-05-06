package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;


public enum AdapterMode {

    /**
     * In <b>ONLINE</b> mode all the requests that require repository access is expected to successfully connect to
     * repository. Underlying objects from package {@linkplain org.apache.stanbol.cmsadapter.servicesapi.model.web}
     * are not used for accessing.
     */
    ONLINE,
    /**
     * In <b>TOLERATED OFFLINE</b> mode, a repository connection error will not be thrown instead 
     * existing underlying objects from package {@linkplain org.apache.stanbol.cmsadapter.servicesapi.model.web}
     * will be used.
     */
    TOLERATED_OFFLINE,
    /**
     * In <b>STRICT OFFLINE</b> repository is never accessed. All the information is expected to provided by
     * underlying objects from package {@linkplain org.apache.stanbol.cmsadapter.servicesapi.model.web}.
     */
    STRICT_OFFLINE

}