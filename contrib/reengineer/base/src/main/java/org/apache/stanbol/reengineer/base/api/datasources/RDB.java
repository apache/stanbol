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
package org.apache.stanbol.reengineer.base.api.datasources;

import org.apache.stanbol.reengineer.base.api.IdentifiedDataSource;
import org.apache.stanbol.reengineer.base.api.settings.ConnectionSettings;
import org.apache.stanbol.reengineer.base.api.util.ReengineerType;
import org.apache.stanbol.reengineer.base.api.util.URIGenerator;

/**
 * An object representing a relational database.
 * 
 * @author andrea.nuzzolese
 * 
 */
public class RDB extends IdentifiedDataSource {

    private ConnectionSettings connectionSettings;

    /**
     * The constructor requires all the parameters in order to establish a connection with the physical DB.
     * Those information regarding the connection with the DB are passed to the constructor in the
     * {@link ConnectionSettings}.
     * 
     * @param connectionSettings
     *            {@link ConnectionSettings}
     */
    public RDB(ConnectionSettings connectionSettings) {
        String dbId = connectionSettings.getUrl() + connectionSettings.getServerName() + ":"
                      + connectionSettings.getPortNumber() + "/" + connectionSettings.getDatabaseName();
        id = URIGenerator.createID("urn:datasource-", dbId.getBytes());
        this.connectionSettings = connectionSettings;
    }

    /**
     * Return the physical data source. In this specific case, as the data source is an RDB, a
     * {@link ConnectionSettings} object containing the information in order to establish a connection with
     * the DB via JDBC is returned
     * 
     * @return the information for establishing the connection with the DB
     */
    @Override
    public Object getDataSource() {
        return connectionSettings;
    }

    /**
     * Return the {@code int} representing the data source type in Semion. In the case of relationa databases
     * the value returned is {@link ReengineerType.RDB}, namely 0.
     * 
     * @return the value assigned to the relational databases by Semion
     */

    @Override
    public int getDataSourceType() {
        return ReengineerType.RDB;
    }

}
