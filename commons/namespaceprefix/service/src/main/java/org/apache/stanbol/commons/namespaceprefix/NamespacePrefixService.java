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
package org.apache.stanbol.commons.namespaceprefix;


/**
 * Namespace prefix mapping service. The {@link #getNamespace(String)} and
 * {@link #getPrefix(String)} methods are intended to be used for
 * exact mappings. The {@link #getFullName(String)} and 
 * {@link #getShortName(String)} to their best to map between URIs and shortNames
 * but do have reasonable defaults if no mappings are present.
 * Users that do want more control over such conversions should use the
 * {@link NamespaceMappingUtils#getNamespace(String)} and 
 * {@link NamespaceMappingUtils#getPrefix(String)} methods and than use the
 * {@link #getNamespace(String)} and {@link #getPrefix(String)} methods on the
 * results.
 * 
 */
public interface NamespacePrefixService extends NamespacePrefixProvider {

    /**
     * Setter for a prefix namespace mapping. If the prefix was already present
     * than the previous mapped namespace is returned. Parsed prefixes
     * are validated using {@link NamespaceMappingUtils#checkPrefix(String)}
     * and namespaces using {@link NamespaceMappingUtils#checkNamespace(String)}
     * @param prefix the prefix
     * @param namespace the namespace. Parsed namespaces MUST end with '/' or '#'
     * or ':' if starting with 'urn:'. Additional validations are optional
     * @return the previous mapped namespace or <code>null</code> if none
     * @throws IllegalArgumentException if the parsed prefix and namespaces are
     * not valid. This is checked by using {@link NamespaceMappingUtils#checkPrefix(String)}
     * and {@link NamespaceMappingUtils#checkNamespace(String)}
     */
    String setPrefix(String prefix, String namespace);
    
    /** 
     * Converts an sort name '{prefix}:{localname}' to the full URI. If an
     * URI is parsed the parsed value is returned unchanged. If the parsed
     * prefix is not known, than <code>null</code> is returned.<p>
     * The detection if a '{prefix}:{localname}' was parsed uses the following rules:<ul>
     * <li> shortname.indexOf(':') gt 0 || shortname.charAt(0) != '/' </li>
     * <li> {prefix} != 'urn' </li>
     * <li> {localname}.charAt(0) != '/' </li>
     * </ul>
     * In case a '{prefix}:{localname}' was detected the parsed value is processed
     * otherwise the parsed value is returned.
     * @param shortNameOrUri Short Name or an URI
     * @return in case an URI was parsed than the parsed value. Otherwise the
     * full URI for the shortName or <code>null</code> if the used {prefix} is
     * not defined.
     */
    String getFullName(String shortNameOrUri);

    /**
     * The short name for the parsed URI.<p>For the detection of the 
     * namespace the last index of '#' or '/' is used. If the namespace is
     * not mapped or the parsed uri does not contain '#' or '/' than the
     * parsed value is returned
     * @param uri the full uri
     * @return the short name in case a prefix mapping for the namespace of the
     * parsed URI was defined. Otherwise the parsed URI is returned.
     */
    String getShortName(String uri);
}
