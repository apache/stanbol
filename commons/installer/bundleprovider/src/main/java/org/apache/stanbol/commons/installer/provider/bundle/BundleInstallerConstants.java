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
package org.apache.stanbol.commons.installer.provider.bundle;

import org.apache.sling.installer.api.InstallableResource;

/**
 * Constants used by the BundleInstaller.
 *
 * @author Rupert Westenthaler
 */
public final class BundleInstallerConstants {

    /**
     * Restrict instantiation
     */
    private BundleInstallerConstants() {}

    /**
     * The name of the header field used for the 
     * <a href="http://www.aqute.biz/Snippets/Extender"> The OSGi extender 
     * pattern </a>.
     */
    public static final String BUNDLE_INSTALLER_HEADER = "Install-Path";

    /**
     * The schema used for {@link InstallableResource}s created by the
     * bundle provider.
     */
    public static final String PROVIDER_SCHEME = "bundleinstall";

}
