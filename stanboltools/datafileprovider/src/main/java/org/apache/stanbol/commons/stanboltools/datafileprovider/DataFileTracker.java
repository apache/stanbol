package org.apache.stanbol.commons.stanboltools.datafileprovider;

import java.util.Map;

public interface DataFileTracker {

    public abstract void add(DataFileListener resourceListener, String name, Map<String,String> propertis);

    public abstract void add(DataFileListener resourceListener, String bundleSymbolicName, String name, Map<String,String> propertis);

    public abstract void remove(DataFileListener resourceListener, String resource);

    public abstract void remove(DataFileListener resourceListener, String bundleSymbolicName, String name);

    public abstract void removeAll(DataFileListener resourceListener);
    
    public boolean isTracked(String bundleSymbolicName,String resourceName);
    
    public boolean isTracked(DataFileListener resourceListener,String bundleSymbolicName,String resourceName);
    
    
}