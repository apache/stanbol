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
package org.apache.stanbol.reengineer.base.api;

/**
 * A {@code DataSource} object represents a physical non-RDF data source in Stanbol Reengineer. <br>
 * <br>
 * Supported data sources are:
 * <ul>
 * <li>Relational databases
 * <li>XML
 * <li>iCalendar
 * <li>RSS
 * </ul>
 * 
 * 
 * @author andrea.nuzzolese
 * 
 */

public interface DataSource {

    /**
     * Get the ID of the data source as it is represented in Semion
     * 
     * @return the {@link String} representing the ID of the physical data source in Semion
     */
    String getID();

    /**
     * As a {@code DataSource} is only a representation of the data source in Semion, a method that returns
     * the physical data source is provided.
     * 
     * @return the physical data source
     */
    Object getDataSource();

    /**
     * Data sources that Semion is able to manage have an integer that identifies the type of the data source.
     * 
     * Valid values are:
     * <ul>
     * <li>0 - Relational Databases
     * <li>1 - XML
     * <li>2 - iCalendar
     * <li>3 - RSS
     * </ul>
     * 
     * @return the data source type
     */
    int getDataSourceType();
}
