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
package org.apache.stanbol.commons.stanboltools.datafileprovider.bundle;

import org.osgi.framework.Constants;

/**
 * Constants used by the BundleInstaller.
 *
 * @author Rupert Westenthaler
 */
public interface BundleResourceProviderConstants {

    /**
     * The name of the header field used for the 
     * <a href="http://www.aqute.biz/Snippets/Extender"> OSGi extender 
     * pattern </a>. As value a ',' separated list of paths is expected
     */
    String BUNDLE_DATAFILE_HEADER = "Data-Files";
    /**
     * The priority of the data files provides by the bundle. Files with
     * higher priority will be returned if multiple DataFileProvider can
     * provide the same file. The value MUST BE an integer because it is used
     * as {@link Constants#SERVICE_RANKING}. The default value is zero.
     */
    String BUNDLE_DATAFILES_PRIORITY_HEADER = "Data-Files-Priority";
}
