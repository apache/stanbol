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
package org.apache.stanbol.enhancer.ldpath.function;

import static at.newmedialab.ldpath.util.Collections.concat;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.parseMimeType;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.ldpath.backend.ContentItemBackend;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.api.functions.SelectorFunction;

/**
 * Provides access to the contents stored in {@link Blob}s added as content parts
 * to a contentItem.<p>
 * 
 * @author Rupert Westenthaler
 *
 */
public class ContentFunction extends ContentItemFunction implements SelectorFunction<Resource> {

    Logger log = LoggerFactory.getLogger(ContentFunction.class);
    LiteralFactory lf = LiteralFactory.getInstance();
    
    public ContentFunction(){
        super("content");
    }
    
    @Override
    public Collection<Resource> apply(ContentItemBackend backend, Collection<Resource>... args) throws IllegalArgumentException {
        ContentItem ci = ((ContentItemBackend)backend).getContentItem();
//        Collection<Resource> contexts = args[0];
        Set<String> mimeTypes;
        if(args == null || args.length < 1){
            mimeTypes = null;
        } else {
//TODO: Wait for ld-path to parse the context
//      http://code.google.com/p/ldpath/issues/detail?id=7
//                //1. check if the first parameter is the context
//                if(!args[0].isEmpty() && backend.isURI(args[0].iterator().next())){
//                    contexts = args[0];
//                    if(args.length > 1){ // cut the context from the args
//                        Collection<Resource>[] tmp = new Collection[args.length-1];
//                        System.arraycopy(args, 0, tmp, 0, tmp.length);
//                        args = tmp;
//                    } else {
//                        args = new Collection[]{};
//                    }
//                } else { //use the ContentItem as context
//                    contexts = java.util.Collections.singleton((Resource)ci.getUri());
//                }
            mimeTypes = new HashSet<String>();
            for(Iterator<Resource> params = concat(args).iterator();params.hasNext();){
                Resource param = params.next();
                String mediaTypeString = backend.stringValue(param);
                try {
                    mimeTypes.add(parseMimeType(mediaTypeString).get(null));
                } catch (IllegalArgumentException e) {
                    log.warn(String.format("Invalid mediaType '%s' (based on RFC 2046) parsed!",
                        mediaTypeString),e);
                }
            }
        }
        Collection<Resource> result;
        Blob blob;
        if(mimeTypes == null || mimeTypes.isEmpty()){
            blob = ci.getBlob();
        } else {
            Entry<UriRef,Blob> entry = ContentItemHelper.getBlob(ci, mimeTypes);
            blob = entry != null ? entry.getValue() : null;
        }
        if(blob == null){
           result = java.util.Collections.emptySet();
        } else {
            String charset = blob.getParameter().get("charset");
            try {
                if(charset != null){
                    result = java.util.Collections.singleton(
                        backend.createLiteral(IOUtils.toString(blob.getStream(), charset)));
                } else { //binary content
                    byte[] data = IOUtils.toByteArray(blob.getStream());
                    result = java.util.Collections.singleton(
                        (Resource)lf.createTypedLiteral(data));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read contents from Blob '"
                    + blob.getMimeType()+"' of ContentItem "+ci.getUri(),e); 
            }
        }
        return result;
    }

}
