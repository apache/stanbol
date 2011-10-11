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
package org.apache.stanbol.reengineer.base.api.settings;

import java.io.Serializable;

/**
 * A {@code ConnectionSettings} contains all the information that are needed in order to open a connection
 * with a relational database through JDBC.
 * 
 * @author andrea.nuzzolese
 * 
 */
public interface ConnectionSettings extends Serializable {

    /**
     * Get the URL of the connection.
     * 
     * @return the URL of the connection as a {@link String}.
     */
    String getUrl();

    /**
     * Get the name of the server on which the DB is running.
     * 
     * @return the name of the server as a {@link String}.
     */
    String getServerName();

    /**
     * Get the port of the server on which the DB is running.
     * 
     * @return the port of the server as a {@link String}.
     */
    String getPortNumber();

    /**
     * Get the name of the database.
     * 
     * @return the port of the server as a {@link String}.
     */
    String getDatabaseName();

    /**
     * Get the user name for the autenthication.
     * 
     * @return the user name as a {@link String}.
     */
    String getUserName();

    /**
     * Get the password for the autenthication.
     * 
     * @return the password as a {@link String}.
     */
    String getPassword();

    /**
     * Get the select method for querying.
     * 
     * @return the select method as a {@link String}.
     */
    String getSelectMethod();

    /**
     * Get the JDBC driver of the database.
     * 
     * @return the JDBC driver as a {@link String}.
     */
    String getJDBCDriver();

}
