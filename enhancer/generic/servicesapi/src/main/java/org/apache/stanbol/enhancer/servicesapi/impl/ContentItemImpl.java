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
package org.apache.stanbol.enhancer.servicesapi.impl;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;


/** 
 * A generic ContentItem implementation that takes the uri, main content part
 * and the graph used to store the metadata as parameter.
 * <p>
 * This content item consisting initially of a single blob. 
 * Subclasses don't have to care about multi-parts aspects of content item. 
 * By inheriting from this class the ability for clients to add additional parts 
 * is ensured. 
 * <p>
 * Even through this class does implement the full {@link ContentItem} interface
 * it is marked as abstract and has only a protected constructor because it is
 * not intended that users directly instantiate it. The intended usage is to
 * create subclasses that instantiate ContentItmes with specific combinations
 * of {@link Blob} nad {@link Graph} implementations.<p>
 * Examples are: <ul>
 * <li>The {@link InMemoryContentItem} intended for in-memory
 * storage of ContentItems during the stateless enhancement workflow
 * <li> The {@link WebContentItem} that allows to create a ContentItem from an 
 * URI.
 * </ul>
 * TODO (rwesten): check if we want this to be an abstract class or if there are
 * reasons to have a general purpose ContentItem implementation
 */
public abstract class ContentItemImpl implements ContentItem {
    
    protected static final String MAIN_BLOB_SUFFIX = "_main";

    /**
     * Holds the content parts of this ContentItem
     */
	private final Map<IRI, Object> parts = new LinkedHashMap<IRI, Object>();
	/**
	 * The uri of the ContentItem
	 */
	private final IRI uri;
	/**
	 * The uri of the main content part (the {@link Blob} parsed with the constructor)
	 */
	private final IRI mainBlobUri;

    private final Graph metadata; 

    protected final Lock readLock;
    protected final Lock writeLock;
    
	protected ContentItemImpl(IRI uri, Blob main, Graph metadata) {
	    if(uri == null){
	        throw new IllegalArgumentException("The URI for the ContentItem MUST NOT be NULL!");
	    }
	    if(main == null){
	        throw new IllegalArgumentException("The main Blob MUST NOT be NULL!");
	    }
	    if(metadata == null){
	        throw new IllegalArgumentException("Tha parsed graph MUST NOT be NULL!");
	    }
        this.uri = uri;
        this.mainBlobUri = new IRI(uri.getUnicodeString()+MAIN_BLOB_SUFFIX);
        this.parts.put(mainBlobUri, main);
        this.metadata = metadata;
        //init the read and write lock
        this.readLock = this.metadata.getLock().readLock();
        this.writeLock = this.metadata.getLock().writeLock();
		//Better parse the Blob in the Constructor than calling a public
		//method on a may be not fully initialised instance
		//parts.put(new IRI(uri.getUnicodeString()+"_main"), getBlob());
	}
	
	@Override
	public final ReadWriteLock getLock() {
	    return metadata.getLock();
	}
	
	/**
	 * Final getter retrieving the Blob via {@link #getPart(IRI, Class)}
	 * with <code>{@link #getUri()}+{@link #MAIN_BLOB_SUFFIX}</code>
	 */
	@Override
	public final Blob getBlob() {
	    readLock.lock();
	    try {
	        return (Blob) parts.get(mainBlobUri);
	    }finally {
	        readLock.unlock();
	    }
	}
	@Override
	public final InputStream getStream() {
        return getBlob().getStream();
	}
    @Override
    public final String getMimeType() {
        return getBlob().getMimeType();
    }
	
    @SuppressWarnings("unchecked")
	@Override
	public <T> T getPart(IRI uri, Class<T> clazz) throws NoSuchPartException {
        readLock.lock();
        try {
            Object part = parts.get(uri);
            if(part == null){
                throw new NoSuchPartException(uri);
            }
            if(clazz.isAssignableFrom(part.getClass())){
                return (T)part;
            } else {
                throw new ClassCastException("The part '"+part+"'(class: "
                        + part.getClass()+") is not compatiple to the requested"
                        + "type "+clazz);
            }
        }finally {
            readLock.unlock();
        }
	}

	@Override
	public IRI getPartUri(int index) throws NoSuchPartException {
        readLock.lock();
        try {
    		int count = 0;
    		for(Map.Entry<IRI, Object> entry : parts.entrySet()) {
    			if (count == index) {
    				return entry.getKey();
    			}
    			count++;
    		}
        } finally {
            readLock.unlock();
        }
  		throw new NoSuchPartException(index);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getPart(int index, Class<T> clazz) throws NoSuchPartException {
        readLock.lock();
        try {
    		Object result = null;
    		int count = 0;
    		for(Map.Entry<IRI, Object> entry : parts.entrySet()) {
    			if (count == index) {
    				result = entry.getValue();
    				if (!result.getClass().isAssignableFrom(clazz)) {
    					throw new NoSuchPartException("The body part 0 is of type "+result.getClass().getName()+" which cannot be converted to "+clazz.getName());
    				}
    				return (T) result;
    			}
    			count++;
    		}
        } finally {
            readLock.unlock();
        }
   		throw new NoSuchPartException(index);
	}
	
	@Override
	public Object addPart(IRI uriRef, Object object) {
        writeLock.lock();
        try {
    	    if(uriRef == null || object == null){
    	        throw new IllegalArgumentException("The parsed content part ID and " +
    	        		"object MUST NOT be NULL!");
    	    }
    	    if(uriRef.equals(mainBlobUri)){ //avoid that this method is used to
    	        //reset the main content part
    	        throw new IllegalArgumentException("The parsed content part ID MUST " +
    	        		"NOT be equals to the ID used by the main Content Part " +
    	        		"( ContentItem.getUri()+\"_main\")");
    	    }
    		return parts.put(uriRef, object);
        } finally {
		    writeLock.unlock();
		}
	}
	@Override
	public void removePart(int index) {
	    if(index < 0) {
	        throw new IllegalArgumentException("The parsed index MUST NOT be < 0");
	    }
	    if(index == 0){
	        throw new IllegalStateException("The main ContentPart (index == 0) CAN NOT be removed!");
	    }
        writeLock.lock();
        try {
            IRI partUri = getPartUri(index);
            parts.remove(partUri);
        } finally {
            writeLock.unlock();
        }
	}
	@Override
	public void removePart(IRI uriRef) {
	    if(uriRef == null){
	        throw new IllegalArgumentException("The parsed uriRef MUST NOT be NULL!");
	    }
        writeLock.lock();
        try {
            IRI mainContentPartUri = parts.keySet().iterator().next();
            if(uriRef.equals(mainContentPartUri)){
                throw new IllegalStateException("The main ContentPart (uri '"
                    + uriRef+"') CAN NOT be removed!");
            }
            if(parts.remove(uriRef) == null){
                throw new NoSuchPartException(uriRef);
            }
        } finally {
            writeLock.unlock();
        }
	}
	
    @Override
	public IRI getUri() {
		return uri;
	}

	@Override
	public Graph getMetadata() {
	    return metadata;
	}
	@Override
	public int hashCode() {
	    return uri.hashCode();
	}
	@Override
	public boolean equals(Object o) {
	    //TODO: is it OK to check only for the uri? An implementation that takes
	    //      the uri, metadata and all content parts into account might be
	    //      to expensive for most common use cases.
	    return o instanceof ContentItem && //check type
	            ((ContentItem)o).getUri().equals(uri);
	}
    @Override
    public String toString() {
        return String.format("%s uri=[%s], content=[%s;mime-type:%s%s], metadata=[%s triples], " +
        		"parts=%s", 
            getClass().getSimpleName(), //the implementation
            getUri().getUnicodeString(), //the URI
            //the size in Bytes (if available)
            getBlob().getContentLength()>=0 ?("size:"+getBlob().getContentLength()+" bytes;") : "",
            getBlob().getMimeType(), //the mime-type
            //and parameter (if available)
            getBlob().getParameter().isEmpty() ? "" : (";parameter:"+getBlob().getParameter()),
            getMetadata().size(), //the number of triples
            parts.keySet()); //and the part URIs
    }

}
