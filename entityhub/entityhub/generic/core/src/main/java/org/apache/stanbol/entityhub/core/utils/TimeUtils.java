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
package org.apache.stanbol.entityhub.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Utilities to parse/format Date and Time values for java {@link Date} objects.<p>
 * Note that <ul>
 * <li> all toString(..) methods format the parsed date in with time zone UTC
 * <li> all toDate(..) methods correctly parse dates in any time zone
 * <li> for {@link DataTypeEnum#DateTime} a parser with Date + optional Time is
 *      used. Meaning that also dates with missing Time Element are excepted
 * </ul>
 *
 * @author Rupert Westenthaler
 *
 */
public final class TimeUtils {
    private TimeUtils(){}

    protected static final Logger log = LoggerFactory.getLogger(TimeUtils.class);
    /**
     * Holds all the data types that represent a date or a time!
     */
    private static final EnumSet<DataTypeEnum> dateOrTimeDataTypes =
        EnumSet.of(DataTypeEnum.DateTime, DataTypeEnum.Time, DataTypeEnum.Date);
    /**
     * ShortNames of the supported date or time dataTypes. Only used to write
     * meaning full error messages if unsupported data types are parsed!
     */
    private static final Collection<String> dateTimeFormatShortNames = new ArrayList<String>(dateOrTimeDataTypes.size());
    static {
        for(DataTypeEnum dateOrTimeDataType : dateOrTimeDataTypes){
            dateTimeFormatShortNames.add(dateOrTimeDataType.getShortName());
        }
    }
    /**
     * Used to encode XML DateTime strings with UTC time zone as used for
     * {@link DataTypeEnum#DateTime}
     */
    protected static final DateTimeFormatter XML_DATE_TIME_FORMAT = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    /**
     * The strict xsd:dateTime parser. It accepts only dateTimes that define
     * all elements. Only the "fraction of second" part and the time zone
     * are optional.<p>
     * This parser is used for {@link DataTypeEnum#DateTime} if
     * <code>strict=true</code> (default is <code>false</code>)
     */
    protected static final DateTimeFormatter XML_DATE_TIME_PARSER_STRICT = ISODateTimeFormat.basicDateTime();
    /**
     * The default parser for {@link DataTypeEnum#DateTime}.<p>
     * This parser not confirm to xsd:dateTime - that requires both date and time
     * to be present - however the parsed value will be the beginning of the
     * period (e.g. 2010-05-05 will be parsed to 2010-05-05T00:00:00.000Z).
     * This is usually the intension of users that uses dateTimes with a missing
     * time. One can parse <code>strict=true</code> to use the
     * {@link #XML_DATE_TIME_PARSER_STRICT} instead.
     */
    protected static final DateTimeFormatter XML_DATE_TIME_PARSER = ISODateTimeFormat.dateOptionalTimeParser();
   /**
     * Used to parse dateTime, date or time from a string<p>
     * Based on the documentation of JodaTime this accepts all possible XML DateTimes
     * <code><pre>
     *      datetime          = time | date-opt-time
     *     time              = 'T' time-element [offset]
     *     date-opt-time     = date-element ['T' [time-element] [offset]]
     *     date-element      = std-date-element | ord-date-element | week-date-element
     *     std-date-element  = yyyy ['-' MM ['-' dd]]
     *     ord-date-element  = yyyy ['-' DDD]
     *     week-date-element = xxxx '-W' ww ['-' e]
     *     time-element      = HH [minute-element] | [fraction]
     *     minute-element    = ':' mm [second-element] | [fraction]
     *     second-element    = ':' ss [fraction]
     *     fraction          = ('.' | ',') digit+
     *     offset            = 'Z' | (('+' | '-') HH [':' mm [':' ss [('.' | ',') SSS]]])
     * <pre><code>
     */
    protected static final DateTimeFormatter XML_DATE_TIME_OR_DATE_OR_TIME_PARSER = ISODateTimeFormat.dateTimeParser();
    /**
     * Used to encode XML DateTime strings without milliseconds with UTC time zone.<p>
     * This can be use full if writing MPEG-7 times, because this std. uses a
     * Format that allows fractions other than milliseconds
     */
    protected static final DateTimeFormatter XML_DATE_TIME_FORMAT_noMillis = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);
    /**
     * used to encode XML Time string with UTC time zone as used by
     * {@link DataTypeEnum#Time}
     */
    protected static final DateTimeFormatter XML_TIME_FORMAT = ISODateTimeFormat.time().withZone(DateTimeZone.UTC);
    /**
     * Used to parse String claiming to be of type {@link DataTypeEnum#Time}<p>
     * Based on the Joda Time documentation this parser accepts:
     * <code><pre>
     *   time           = ['T'] time-element [offset]
     *   time-element   = HH [minute-element] | [fraction]
     *   minute-element = ':' mm [second-element] | [fraction]
     *   second-element = ':' ss [fraction]
     *   fraction       = ('.' | ',') digit+
     *   offset         = 'Z' | (('+' | '-') HH [':' mm [':' ss [('.' | ',') SSS]]])
     * </pre><code>
     */
    protected static final DateTimeFormatter XML_TIME_PARSER = ISODateTimeFormat.timeParser();
    /**
     * used to encode XML Date strings with UTC time zone as used by
     * {@link DataTypeEnum#Date};
     */
    protected static final DateTimeFormatter XML_DATE_FORMAT = ISODateTimeFormat.date().withZone(DateTimeZone.UTC);
    /**
     * Used to parse String claiming to be of type {@link DataTypeEnum#Date}<p>
     * Based on the Joda Time documentation this parser accepts:
     * <code><pre>
     *   date              = date-element ['T' offset]
     *   date-element      = std-date-element | ord-date-element | week-date-element
     *   std-date-element  = yyyy ['-' MM ['-' dd]]
     *   ord-date-element  = yyyy ['-' DDD]
     *   week-date-element = xxxx '-W' ww ['-' e]
     *   offset            = 'Z' | (('+' | '-') HH [':' mm [':' ss [('.' | ',') SSS]]])
     * </pre><code>
     */
    protected static final DateTimeFormatter XML_DATE_PARSER = ISODateTimeFormat.dateParser();
// TODO Future support for all XML date and time formats
//    protected static final DateTimeFormatter XML_gYear_FORMAT = ISODateTimeFormat.year().withZone(DateTimeZone.UTC);
//    protected static final DateTimeFormatter XML_gYearMonth_FORMAT = ISODateTimeFormat.yearMonth().withZone(DateTimeZone.UTC);

    /**
     * Lazy initialisation to avoid Exceptions if {@link DatatypeConfigurationException}
     * is thrown during initialisation of the Utility class.<p>
     * Do not access directly! Use {@link #getXmlDataTypeFactory()} instead.
     */
    private static DatatypeFactory xmlDatatypeFactory;
    /**
     * Inits the {@link #xmlDatatypeFactory} if not already done.<p>
     * @return the XML datatype factory
     * @throws IllegalStateException if a {@link DatatypeConfigurationException}
     * is encountered during {@link DatatypeFactory#newInstance()}
     */
    private static DatatypeFactory getXmlDataTypeFactory() throws IllegalStateException {
        if(xmlDatatypeFactory == null){
            try {
                xmlDatatypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
                throw new IllegalStateException("Unable to instantiate XML Datatype Factory!",e);
            }
        }
        return xmlDatatypeFactory;
    }

    public static String toString(String dataTypeUri,Date value){
        DataTypeEnum dataType = DataTypeEnum.getDataType(dataTypeUri);
        if(dataType == null){
            throw new IllegalArgumentException(String.format("Unknown dataType %s",dataType));
        }
        return toString(dataType,value);
    }
    public static String toString(DataTypeEnum dataType,Date value){
        if(value == null){
            throw new IllegalArgumentException("The parsed Date MUST NOT be NULL!");
        }
        if(dataType == null){
            throw new IllegalArgumentException("The parsed DataType MUST NOT be NULL!");
        }
        if(!dateOrTimeDataTypes.contains(dataType)){
            throw new IllegalArgumentException(String.format("The parsed DataType %s is not a date and/or time dataType! Supported dataTypes are %s",
                    dataType.getShortName(),dateTimeFormatShortNames));
        }
        if(DataTypeEnum.DateTime == dataType){
            return XML_DATE_TIME_FORMAT.print(value.getTime());
        } else if(DataTypeEnum.Time == dataType){
            return XML_TIME_FORMAT.print(value.getTime());
        } else if(DataTypeEnum.Date == dataType){
            return XML_DATE_FORMAT.print(value.getTime());
        } else {
            throw new IllegalArgumentException(String.format("Unsupported but valid Date/Time DataType %s encountered. Pleas report this as a BUG!",dataType));
        }
    }
    /**
     * Converts the parsed value to a Date
     * @param dataType the dataType of Date that should be parsed form the parsed value
     * @param value the value
     * @return the date
     * @throws IllegalArgumentException if the parsed value can not be converted to a date
     */
    public static Date toDate(String dataTypeUri, Object value){
        return toDate(dataTypeUri, value, false);
    }
    /**
     * Converts the parsed value to a Date
     * @param dataType the dataType of Date that should be parsed form the parsed value
     * @param value the value
     * @param strict if <code>true</code> than all requirements defined by xsd are
     * enforced (e.g. dateTimes without time will throw an exception)
     * @return the date
     * @throws IllegalArgumentException if the parsed value can not be converted to a date
     */
    public static Date toDate(String dataTypeUri, Object value, boolean strict){
           DataTypeEnum dataType = DataTypeEnum.getDataType(dataTypeUri);
        if(dataType == null){
            throw new IllegalArgumentException(String.format("Unknown dataType %s",dataType));
        }
        return toDate(dataType, value,strict);
    }
    /**
     * Converts the parsed value to a Date
     * @param dataType the dataType of Date that should be parsed form the parsed value
     * @param value the value
     * @return the date
     * @throws IllegalArgumentException if the parsed value can not be converted to a date
     */
    public static Date toDate(DataTypeEnum dataType, Object value) throws IllegalArgumentException {
        return toDate(dataType, value, false);
    }
    /**
     * Converts the parsed value to a Date
     * @param dataType the dataType of Date that should be parsed form the parsed value
     * @param value the value
     * @param strict if <code>true</code> than all requirements defined by xsd are
     * enforced (e.g. dateTimes without time will throw an exception)
     * @return the date
     * @throws IllegalArgumentException if the parsed value can not be converted to a date
     */
    public static Date toDate(DataTypeEnum dataType, Object value, boolean strict) throws IllegalArgumentException {
        DateTime dateTime = toDateTime(dataType, value, strict);
        return dateTime.toDate();
    }
    /**
     * Converts the value to a xml Gregorian calendar by using
     * {@link DatatypeFactory#newXMLGregorianCalendar(String)}.
     * @param value the value
     * @return the parsed instance
     * @throws IllegalArgumentException if <code>null</code> is parsed
     * @throws IllegalStateException if no {@link DatatypeFactory} could be
     * instantiated.
     */
    public static XMLGregorianCalendar toXMLCal(Object value) throws IllegalArgumentException,IllegalStateException{
        if(value == null){
            throw new IllegalArgumentException("The parsed value MUST NOT be NULL!");
        }
        return getXmlDataTypeFactory().newXMLGregorianCalendar(value.toString());
    }

    public static Duration toDuration(Object value) throws IllegalArgumentException,IllegalStateException{
        return toDuration(value,false);
    }
    public static Duration toDuration(Object value,boolean nullAsZeroDuration) throws IllegalArgumentException,IllegalStateException{
        if(value == null){
            if(nullAsZeroDuration){
                return getXmlDataTypeFactory().newDuration(0);
            } else {
                throw new IllegalArgumentException("The parsed value MUST NOT be NULL. Parse \"boolean nullAsZeroDuration=true\" to enable creation of zero lenght durations for NULL values!");
            }
        } else {
            return getXmlDataTypeFactory().newDuration(value.toString());
        }
    }

    private static DateTime toDateTime(DataTypeEnum dataType, Object value, boolean strict) throws IllegalArgumentException {
           if(value == null){
            throw new IllegalArgumentException("The parsed Date MUST NOT be NULL!");
        }
        if(dataType == null){
            throw new IllegalArgumentException("The parsed DataType MUST NOT be NULL!");
        }
        if(!dateOrTimeDataTypes.contains(dataType)){
            throw new IllegalArgumentException(String.format("The parsed DataType %s is not a date and/or time dataType! Supported dataTypes are %s",
                    dataType.getShortName(),dateTimeFormatShortNames));
        }
        final DateTime dateTime;
        if(value instanceof Date){ //NOTE: returns a valid date for non date dataTypes
            dateTime = new DateTime(((Date)value).getTime());
        } else if(value instanceof DateTime){//NOTE: returns a valid date for non date dataTypes
            dateTime = (DateTime)value;
        } else {
            if(DataTypeEnum.DateTime == dataType){
                   if(strict){
                       dateTime = XML_DATE_TIME_PARSER_STRICT.parseDateTime(value.toString());
                   } else {
                       dateTime = XML_DATE_TIME_PARSER.parseDateTime(value.toString());
                   }
            } else if(DataTypeEnum.Time == dataType){
                dateTime =  XML_TIME_PARSER.parseDateTime(value.toString());
            } else if(DataTypeEnum.Date == dataType){
                dateTime =  XML_DATE_PARSER.parseDateTime(value.toString());
            } else {
                String strValue = value.toString();
                log.error(String.format("Unsupported but valid Date/Time DataType %s encountered. Pleas report this as a BUG!",dataType));
                log.warn(String.format("Try to use the generic dateTime-or-Time-or-Date parser as fallback for unsupported but valid Date/Time DataType %s and value %s",
                        dataType,strValue));
                dateTime = XML_DATE_TIME_OR_DATE_OR_TIME_PARSER.parseDateTime(strValue);
            }
        }
        return dateTime;
    }
}
