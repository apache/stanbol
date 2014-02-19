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
package org.apache.stanbol.ontologymanager.store.jena.util;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.XSD;
import com.hp.hpl.jena.xmloutput.impl.BaseXMLWriter;
import com.hp.hpl.jena.xmloutput.impl.SimpleLogger;

public class JenaUtil {

    public static boolean isBuiltInClass(String resourceURI) {
        if ((OWL.Thing).getURI().equalsIgnoreCase(resourceURI)
            || (OWL.Nothing).getURI().equalsIgnoreCase(resourceURI)) return true;
        else return false;
    }

    public static Resource getBuiltInClass(String resourceURI) {
        if ((OWL.Thing).getURI().equalsIgnoreCase(resourceURI)) return OWL.Thing;
        if ((OWL.Nothing).getURI().equalsIgnoreCase(resourceURI)) return OWL.Nothing;
        return null;
    }

    public static boolean isBuiltInType(String resourceURI) {
        int cutIndex = resourceURI.lastIndexOf("/");
        resourceURI = resourceURI.substring(cutIndex);
        // FIXME:: check if the prefix is a valid reference to XSD namespace
        if ((XSD.anyURI).getURI().contains(resourceURI)
            || (XSD.base64Binary).getURI().contains(resourceURI)
            || (XSD.date).getURI().contains(resourceURI)
            || (XSD.dateTime).getURI().contains(resourceURI)
            || (XSD.decimal).getURI().contains(resourceURI)
            || (XSD.duration).getURI().contains(resourceURI)
            ||
            (XSD.ENTITY).getURI().contains(resourceURI)
            || (XSD.gDay).getURI().contains(resourceURI)
            || (XSD.gMonth).getURI().contains(resourceURI)
            || (XSD.gMonthDay).getURI().contains(resourceURI)
            || (XSD.gYear).getURI().contains(resourceURI)
            || (XSD.gYearMonth).getURI().contains(resourceURI)
            || (XSD.hexBinary).getURI().contains(resourceURI)
            || (XSD.ID).getURI().contains(resourceURI)
            || (XSD.IDREF).getURI().contains(resourceURI)
            ||
            (XSD.integer).getURI().contains(resourceURI)
            || (XSD.language).getURI().contains(resourceURI)
            || (XSD.Name).getURI().contains(resourceURI)
            || (XSD.NCName).getURI().contains(resourceURI)
            || (XSD.negativeInteger).getURI().contains(resourceURI)
            || (XSD.NMTOKEN).getURI().contains(resourceURI)
            ||
            (XSD.nonNegativeInteger).getURI().contains(resourceURI)
            || (XSD.nonPositiveInteger).getURI().contains(resourceURI)
            || (XSD.normalizedString).getURI().contains(resourceURI)
            || (XSD.NOTATION).getURI().contains(resourceURI)
            || (XSD.positiveInteger).getURI().contains(resourceURI)
            || (XSD.QName).getURI().contains(resourceURI) || (XSD.time).getURI().contains(resourceURI)
            || (XSD.token).getURI().contains(resourceURI)
            || (XSD.unsignedByte).getURI().contains(resourceURI)
            || (XSD.unsignedInt).getURI().contains(resourceURI)
            || (XSD.unsignedLong).getURI().contains(resourceURI)
            || (XSD.unsignedShort).getURI().contains(resourceURI)
            || (XSD.xboolean).getURI().contains(resourceURI) || (XSD.xbyte).getURI().contains(resourceURI)
            || (XSD.xdouble).getURI().contains(resourceURI) || (XSD.xfloat).getURI().contains(resourceURI)
            || (XSD.xint).getURI().contains(resourceURI) || (XSD.xlong).getURI().contains(resourceURI)
            || (XSD.xshort).getURI().contains(resourceURI) || (XSD.xstring).getURI().contains(resourceURI)) return true;
        else return false;
    }

    public static Resource getResourceForBuiltInType(String resourceURI) {
        if ((XSD.anyURI).getURI().contains(resourceURI)) return XSD.anyURI;
        if ((XSD.base64Binary).getURI().contains(resourceURI)) return XSD.base64Binary;
        if ((XSD.date).getURI().contains(resourceURI)) return XSD.date;
        if ((XSD.dateTime).getURI().contains(resourceURI)) return XSD.dateTime;
        if ((XSD.decimal).getURI().contains(resourceURI)) return XSD.decimal;
        if ((XSD.duration).getURI().contains(resourceURI)) return XSD.duration;
        if ((XSD.ENTITY).getURI().contains(resourceURI)) return XSD.ENTITY;
        if ((XSD.gDay).getURI().contains(resourceURI)) return XSD.gDay;
        if ((XSD.gMonth).getURI().contains(resourceURI)) return XSD.gMonth;
        if ((XSD.gMonthDay).getURI().contains(resourceURI)) return XSD.gMonthDay;
        if ((XSD.gYear).getURI().contains(resourceURI)) return XSD.gYear;
        if ((XSD.gYearMonth).getURI().contains(resourceURI)) return XSD.gYearMonth;
        if ((XSD.hexBinary).getURI().contains(resourceURI)) return XSD.hexBinary;
        if ((XSD.ID).getURI().contains(resourceURI)) return XSD.ID;
        if ((XSD.IDREF).getURI().contains(resourceURI)) return XSD.IDREF;
        if ((XSD.integer).getURI().contains(resourceURI)) return XSD.integer;
        if ((XSD.language).getURI().contains(resourceURI)) return XSD.language;
        if ((XSD.Name).getURI().contains(resourceURI)) return XSD.Name;
        if ((XSD.NCName).getURI().contains(resourceURI)) return XSD.NCName;
        if ((XSD.negativeInteger).getURI().contains(resourceURI)) return XSD.negativeInteger;
        if ((XSD.NMTOKEN).getURI().contains(resourceURI)) return XSD.NMTOKEN;
        if ((XSD.nonNegativeInteger).getURI().contains(resourceURI)) return XSD.nonNegativeInteger;
        if ((XSD.nonPositiveInteger).getURI().contains(resourceURI)) return XSD.nonPositiveInteger;
        if ((XSD.normalizedString).getURI().contains(resourceURI)) return XSD.normalizedString;
        if ((XSD.NOTATION).getURI().contains(resourceURI)) return XSD.NOTATION;
        if ((XSD.positiveInteger).getURI().contains(resourceURI)) return XSD.positiveInteger;
        if ((XSD.QName).getURI().contains(resourceURI)) return XSD.QName;
        if ((XSD.time).getURI().contains(resourceURI)) return XSD.time;
        if ((XSD.token).getURI().contains(resourceURI)) return XSD.token;
        if ((XSD.unsignedByte).getURI().contains(resourceURI)) return XSD.unsignedByte;
        if ((XSD.unsignedInt).getURI().contains(resourceURI)) return XSD.unsignedInt;
        if ((XSD.unsignedLong).getURI().contains(resourceURI)) return XSD.unsignedLong;
        if ((XSD.unsignedShort).getURI().contains(resourceURI)) return XSD.unsignedShort;
        if ((XSD.xboolean).getURI().contains(resourceURI)) return XSD.xboolean;
        if ((XSD.xbyte).getURI().contains(resourceURI)) return XSD.xbyte;
        if ((XSD.xdouble).getURI().contains(resourceURI)) return XSD.xdouble;
        if ((XSD.xfloat).getURI().contains(resourceURI)) return XSD.xfloat;
        if ((XSD.xint).getURI().contains(resourceURI)) return XSD.xint;
        if ((XSD.xlong).getURI().contains(resourceURI)) return XSD.xlong;
        if ((XSD.xshort).getURI().contains(resourceURI)) return XSD.xshort;
        if ((XSD.xstring).getURI().contains(resourceURI)) return XSD.xstring;
        return null;
    }

    // For setting log levels etc.
    public static void initialConf() {
        SimpleLogger logger = new SimpleLogger() {

            @Override
            public void warn(String s, Exception e) {

            }

            @Override
            public void warn(String s) {

            }
        };
        BaseXMLWriter.setLogger(logger);
    }

}
