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
package org.apache.stanbol.enhancer.topic;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Utilities to compute UTC timestamp to make Solr queries independent of the locale of the server.
 */
public final class UTCTimeStamper {

    /**
     * Restrict instantiation
     */
    private UTCTimeStamper() {}

    protected static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * @return format a date in the timezone as an UTC ISO 8601 string up to the millisecond precision.
     */
    public static String utcIsoString(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        df.setTimeZone(UTC);
        return df.format(date) + "Z";
    }

    /**
     * @return ISO 8601 serialization of the current date in the UTC timezone suitable for range queries on
     *         Solr.
     */
    public static String nowUtcIsoString() {
        return utcIsoString(nowUtcDate());
    }

    /**
     * @return current date in the UTC timezone suitable for storage in a date field of a Solr index.
     */
    public static Date nowUtcDate() {
        return (new GregorianCalendar(UTC)).getTime();
    }
}
