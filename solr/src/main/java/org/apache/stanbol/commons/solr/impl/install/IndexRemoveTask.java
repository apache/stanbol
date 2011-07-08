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
package org.apache.stanbol.commons.solr.impl.install;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.stanbol.commons.solr.SolrDirectoryManager;

/**
 * TODO: To remove a SolrIndex one would need first to close the SolrCore or shutdown the SolrContainer. This
 * is currently not possible be cause the current architecture was not intended to support that.
 * <p>
 * To implement this one would need access to the CoreContainer with the core running on top of the Core to
 * remove. Than one would need to call {@link CoreContainer#remove(String)} with the core and
 * {@link CoreContainer#persist()} to remove the core also from the solr.xml. After that one can remove the
 * files from the disk.
 * <p>
 * This would still have the problem that other components using an {@link EmbeddedSolrServer} that is based
 * on this core would not be notified about such a change.
 * 
 * @author Rupert Westenthaler
 * 
 */
public class IndexRemoveTask extends InstallTask {
    /**
     * Use 11 because the RemoveConfiguration uses 10 and we need to ensure that the files are removed after
     * the services are shut down.
     */
    private static final String CONFIG_INSTALL_ORDER = "11-";

    public IndexRemoveTask(TaskResourceGroup trg, SolrDirectoryManager solrDirectoryManager) {
        super(trg);
    }

    @Override
    public void execute(InstallationContext ctx) {
        throw new UnsupportedOperationException("TODO: Not yet implemented :(");
    }

    @Override
    public String getSortKey() {
        return CONFIG_INSTALL_ORDER + getResource().getEntityId();
    }

}
