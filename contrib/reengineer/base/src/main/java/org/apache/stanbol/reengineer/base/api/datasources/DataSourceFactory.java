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

import java.io.InputStream;

import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.settings.ConnectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceFactory {

    private static Logger log = LoggerFactory.getLogger(DataSourceFactory.class);

    public static DataSource createDataSource(int dataSourceType, Object source) throws NoSuchDataSourceExpection,
                                                                                InvalidDataSourceForTypeSelectedException {
        DataSource dataSource;
        log.debug("Created data source for object of type {}", source.getClass());
        switch (dataSourceType) {
            case 0:
                if (source instanceof ConnectionSettings) {
                    dataSource = new RDB((ConnectionSettings) source);
                } else {
                    throw new InvalidDataSourceForTypeSelectedException(source);
                }
                break;
            case 1:
                if (source instanceof InputStream) {
                    dataSource = new XML((InputStream) source);
                } else {
                    throw new InvalidDataSourceForTypeSelectedException(source);
                }
                break;

            default:
                throw new NoSuchDataSourceExpection(dataSourceType);
        }

        return dataSource;
    }
}
