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
package org.apache.stanbol.entityhub.servicesapi.defaults;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
/**
 * Defines the data types that need to be supported by the model 
 * implementation.<p>
 * Each data type defines:<ul>
 * <li><b>short name:</b> An short and human readable ID that is unique within 
 * the list of data types. Currently <code>prefix:localName</code> is used as 
 * short name and prefixes are used as defined by the {@link NamespaceEnum}.
 * <li><b>uri:</b> Ths is the global unique UD of the namespace. If possible the
 * URI defined by XSD is used (e.g. http://www.w3.org/2001/XMLSchema#string for
 * strings). 
 * <li><b>java class:</b> Each data type is mapped to exactly one preferred 
 * and 0..n optional java representations. Note that
 * different data types may use the same preferred as well as optional java class 
 * meaning. This means that the java class can not be used to uniquely identify 
 * a data type.
 * </ul>
 * The {@link #name()} is not used, but typically the local name with an capital
 * first letter is used. The URI of the data type is used by the implementation
 * of {@link #toString()}.<p>
 * In addition the the definition of all the data types this class also provides
 * several utilities for getting the data type by short name, URI as well as
 * preferred or all defined java class mappings.
 * 
 * @author Rupert Westenthaler
 *
 */
public enum DataTypeEnum {
    //Entityhub specific
    Reference(NamespaceEnum.entityhub,"ref",Reference.class),
    Text(NamespaceEnum.entityhub,"text",Text.class),
    //xsd types
    /**
     * currently URIs are preferable mapped to {@link Reference}, because there
     * may be RDF URIs that are not valid {@link URI}s nor {@link URL}s.
     */
    AnyUri("anyURI",Reference.class,URI.class,URL.class),
    Boolean("boolean",Boolean.class),
    Byte("byte",Byte.class),
    Short("short",Short.class),
    Integer("integer",BigInteger.class),
    Decimal("decimal",BigDecimal.class),
    Int("int",Integer.class),
    Long("long",Long.class),
    Float("float",Float.class),
    Double("double",Double.class),
    String("string",String.class,Text.class),
    Time("time",Date.class), //TODO: check if the XML calendar would be better
    Date("date",Date.class),
    DateTime("dateTime",Date.class),
    Duration("duration",Duration.class),
    ;
    private final Class<?> javaType;
    private final QName qName;
    private final String shortName;
    private final String uri;
    private final Set<Class<?>> additional;
    DataTypeEnum(Class<?> javaType, Class<?>... additional){
        this(null,null,javaType,additional);
    }
    DataTypeEnum(String localName, Class<?> javaType, Class<?>... additional){
        this(null,localName,javaType,additional);
    }
    DataTypeEnum(NamespaceEnum namespace, Class<?> javaType, Class<?>... additional) {
        this(namespace,null,javaType,additional);
    }
    DataTypeEnum(NamespaceEnum namespace, String localName, Class<?> javaType, Class<?>... additional) {
        if(namespace == null){
            namespace = NamespaceEnum.xsd;
        }
        if(localName == null){
            localName = name();
        }
        if(additional != null && additional.length>0){
            this.additional = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(additional)));
        } else {
            this.additional = Collections.emptySet();
        }
        this.javaType = javaType;
        this.qName = new QName(namespace.getNamespace(), localName, namespace.getPrefix());
        //a lot of accesses will be based on the Uri and the shortName.
        // -> so store the IDs and the shortName as local Vars.
        this.shortName = qName.getPrefix()+':'+qName.getLocalPart();
        this.uri = qName.getNamespaceURI()+qName.getLocalPart();
    }
    public final String getLocalName() {
        return qName.getLocalPart();
    }
    public final Class<?> getJavaType() {
        return javaType;
    }
    public Set<Class<?>> getAdditionalJavaTypes() {
        return additional;
    }
    public final NamespaceEnum getNamespace() {
        return NamespaceEnum.forNamespace(qName.getNamespaceURI());
    }
    public final String getUri(){
        return uri;
    }
    public final String getShortName(){
        return shortName;
    }
    public final QName getQName(){
        return qName;
    }
    @Override
    public String toString() {
        return getUri();
    }

    private static Map<Class<?>,Set<DataTypeEnum>> class2DataTypeMap;
    private static Map<Class<?>,Set<DataTypeEnum>> interface2DataTypeMap;
    private static Map<Class<?>,Set<DataTypeEnum>> allClass2DataTypeMap;
    private static Map<Class<?>,Set<DataTypeEnum>> allInterface2DataTypeMap;
    private static Map<String,DataTypeEnum> uri2DataType;
    private static Map<String,DataTypeEnum> shortName2DataType;
    static{
        Map<Class<?>,Set<DataTypeEnum>> c2d = new HashMap<Class<?>, Set<DataTypeEnum>>();
        Map<Class<?>,Set<DataTypeEnum>> i2d = new HashMap<Class<?>, Set<DataTypeEnum>>();
        Map<Class<?>,Set<DataTypeEnum>> ac2d = new HashMap<Class<?>, Set<DataTypeEnum>>();
        Map<Class<?>,Set<DataTypeEnum>> ai2d = new HashMap<Class<?>, Set<DataTypeEnum>>();
        Map<String,DataTypeEnum> u2d = new HashMap<String, DataTypeEnum>();
        Map<String,DataTypeEnum> s2d = new HashMap<String, DataTypeEnum>();
        for(DataTypeEnum dataType : DataTypeEnum.values()){
            //add primary javaType -> data type mappings
            if(dataType.javaType.isInterface()){
                Set<DataTypeEnum> dataTypes = i2d.get(dataType.javaType);
                if(dataTypes == null){
                    dataTypes = EnumSet.noneOf(DataTypeEnum.class);
                    i2d.put(dataType.javaType, dataTypes);
                }
                dataTypes.add(dataType);
            } else { //a class
                Set<DataTypeEnum> dataTypes = c2d.get(dataType.javaType);
                if(dataTypes == null){
                    dataTypes = EnumSet.noneOf(DataTypeEnum.class);
                    c2d.put(dataType.javaType, dataTypes);
                }
                dataTypes.add(dataType);
            }
            //add additional javaType -> data type mappings
            for(Class<?> additionalClass : dataType.additional){
                if(additionalClass.isInterface()){
                    Set<DataTypeEnum> dataTypes = ai2d.get(additionalClass);
                    if(dataTypes == null){
                        dataTypes = EnumSet.noneOf(DataTypeEnum.class);
                        ai2d.put(additionalClass, dataTypes);
                    }
                    dataTypes.add(dataType);
                } else { //a class
                    Set<DataTypeEnum> dataTypes = ac2d.get(additionalClass);
                    if(dataTypes == null){
                        dataTypes = EnumSet.noneOf(DataTypeEnum.class);
                        ac2d.put(additionalClass, dataTypes);
                    }
                    dataTypes.add(dataType);
                }
            }
            if(u2d.containsKey(dataType.getUri())){
                throw new IllegalStateException(java.lang.String.format("Invalid configuration in DataTypeEnum because dataType uri %s is used for %s and %s!",
                        dataType.getUri(),dataType.name(),u2d.get(dataType.getUri()).name()));
            }
            u2d.put(dataType.getUri(), dataType);
            if(s2d.containsKey(dataType.getShortName())){
                throw new IllegalStateException(java.lang.String.format("Invalid configuration in DataTypeEnum because dataType short name (prefix:localname) %s is used for %s and %s!",
                        dataType.getShortName(),dataType.name(),s2d.get(dataType.getShortName()).name()));
            }
            s2d.put(dataType.getShortName(), dataType);
        }
        class2DataTypeMap = Collections.unmodifiableMap(c2d);
        interface2DataTypeMap = Collections.unmodifiableMap(i2d);
        allClass2DataTypeMap = Collections.unmodifiableMap(ac2d);
        allInterface2DataTypeMap = Collections.unmodifiableMap(ai2d);
        uri2DataType = Collections.unmodifiableMap(u2d);
        shortName2DataType = Collections.unmodifiableMap(s2d);
    }
    public static Set<DataTypeEnum> getPrimaryDataTypes(Class<?> javaClass){
        Set<DataTypeEnum> dataTypes = EnumSet.noneOf(DataTypeEnum.class);
        Set<DataTypeEnum> classesTypes = class2DataTypeMap.get(javaClass);
        if(classesTypes != null){
            dataTypes.addAll(classesTypes);
        }
        for(Entry<Class<?>, Set<DataTypeEnum>> entry : interface2DataTypeMap.entrySet()){
            if(entry.getKey().isAssignableFrom(javaClass)){
                dataTypes.addAll(entry.getValue());
            }
        }
        return dataTypes;

    }
    public static Set<DataTypeEnum> getAllDataTypes(Class<?> javaClass){
        //start with the primary
        Set<DataTypeEnum> all = getPrimaryDataTypes(javaClass);
        //now add the additional types
        Set<DataTypeEnum> additionalClassesTypes = allClass2DataTypeMap.get(javaClass);
        if(additionalClassesTypes != null){
            all.addAll(additionalClassesTypes);
        }
        for(Entry<Class<?>, Set<DataTypeEnum>> entry : allInterface2DataTypeMap.entrySet()){
            if(entry.getKey().isAssignableFrom(javaClass)){
                all.addAll(entry.getValue());
            }
        }
        return all;
    }
//    public static DataTypeEnum getDataType(Class<?> javaClass){
//        List<DataTypeEnum> dataTypes = getAllDataTypes(javaClass);
//        return dataTypes.isEmpty()?null:dataTypes.get(0);
//    }
    public static DataTypeEnum getDataType(String uri){
        return uri2DataType.get(uri);
    }
    public static DataTypeEnum getDataTypeByShortName(String shortName){
        return shortName2DataType.get(shortName);
    }
}
