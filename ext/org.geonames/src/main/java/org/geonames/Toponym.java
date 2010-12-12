/*
 * Copyright 2008 Marc Wick, geonames.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.geonames;

/**
 * a GeoNames toponym
 * <p>
 * NOTE (Rupert Westenthaler) added get/set for score and DEFAULT_SCORE
 *
 * @author marc@geonames
 *
 */
public class Toponym {

    private int geoNameId;

    private String name;

    private String alternateNames;

    private String countryCode;

    private String countryName;

    private Long population;

    private Integer elevation;

    private FeatureClass featureClass;

    private String featureClassName;

    private String featureCode;

    private String featureCodeName;

    private double latitude;

    private double longitude;

    private String adminCode1;

    private String adminName1;

    private String adminCode2;

    private String adminName2;

    private String adminCode3;

    private String adminCode4;

    private Timezone timezone;

    private Style style;

    private Double score;
    /**
     * Not all services returning toponyms provide a score. For such services
     * (e.g. hierarchy) it is assumed, that the score is 1.0.<br>
     * This default score is only returned if no score information is available
     * AND ({@link Style} <code> == null</code> OR (
     * {@link Style} NOT <code>null</code> AND {@link Style} EQUALS
     * {@link Style#FULL}))<br>
     * Otherwise a {@link InsufficientStyleException} is thrown if no score
     * information is available.
     */
    private static final Double DEFAULT_SCORE = 1.0;

    /**
     * @return Returns the score of this result
     * @throws InsufficientStyleException The style is only available with
     *     {@link Style#FULL}.
     */
    public Double getScore() throws InsufficientStyleException {
        if (score == null && style != null
                && Style.FULL.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "score not supported by style " + style.name());
        }
        return score!=null?score:DEFAULT_SCORE;
    }

    /**
     * @param score
     *            The score of the result
     */
    public void setScore(Double score) {
        this.score = score;
    }

    /**
     * @return Returns the ISO 3166-1-alpha-2 countryCode.
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * @param countryCode
     *            The ISO 3166-1-alpha-2 countryCode to set.
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * @return Returns the elevation in meter.
     */
    public Integer getElevation() throws InsufficientStyleException {
        if (elevation == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "elevation not supported by style " + style.name());
        }
        return elevation;
    }

    /**
     * @param elevation
     *            The elevation im meter to set.
     */
    public void setElevation(Integer elevation) {
        this.elevation = elevation;
    }

    /**
     * the feature class {@link FeatureClass}
     *
     * @see <a href="http://www.geonames.org/export/codes.html">GeoNames Feature
     *      Codes</a>
     * @return Returns the featureClass.
     */
    public FeatureClass getFeatureClass() {
        return featureClass;
    }

    /**
     * @param featureClass
     *            The featureClass to set.
     */
    public void setFeatureClass(FeatureClass featureClass) {
        this.featureClass = featureClass;
    }

    /**
     * @see <a href="http://www.geonames.org/export/codes.html">GeoNames Feature
     *      Codes</a>
     * @return Returns the featureCode.
     */
    public String getFeatureCode() {
        return featureCode;
    }

    /**
     * @param featureCode
     *            The featureCode to set.
     */
    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    /**
     * latitude in decimal degrees (wgs84)
     *
     * @return Returns the latitude.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @param latitude
     *            The latitude to set.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * longitude in decimal degrees (wgs84)
     *
     * @return Returns the longitude.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @param longitude
     *            The longitude to set.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the population.
     */
    public Long getPopulation() throws InsufficientStyleException {
        if (population == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "population not supported by style " + style.name());
        }
        return population;
    }

    /**
     * @param population
     *            The population to set.
     */
    public void setPopulation(Long population) {
        this.population = population;
    }

    /**
     * @return Returns the geoNameId.
     */
    public int getGeoNameId() {
        return geoNameId;
    }

    /**
     * @param geoNameId
     *            The geoNameId to set.
     */
    public void setGeoNameId(int geonameId) {
        this.geoNameId = geonameId;
    }

    /**
     * @return Returns the featureClassName.
     */
    public String getFeatureClassName() {
        return featureClassName;
    }

    /**
     * @param featureClassName
     *            The featureClassName to set.
     */
    public void setFeatureClassName(String featureClassName) {
        this.featureClassName = featureClassName;
    }

    /**
     * @return Returns the featureCodeName.
     */
    public String getFeatureCodeName() {
        return featureCodeName;
    }

    /**
     * @param featureCodeName
     *            The featureCodeName to set.
     */
    public void setFeatureCodeName(String featureCodeName) {
        this.featureCodeName = featureCodeName;
    }

    /**
     * @return Returns the countryName.
     */
    public String getCountryName() {
        return countryName;
    }

    /**
     * @param countryName
     *            The countryName to set.
     */
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    /**
     * alternate names of this place as comma separated list
     *
     * @return the alternateNames as comma separated list
     */
    public String getAlternateNames() throws InsufficientStyleException {
        if (alternateNames == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "alternateNames not supported by style " + style.name());
        }
        return alternateNames;
    }

    /**
     * @param alternateNames
     *            the alternateNames to set
     */
    public void setAlternateNames(String alternateNames) {
        this.alternateNames = alternateNames;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("geoNameId=" + geoNameId + ",");
        str.append("name=" + name + ",");
        if (alternateNames != null) {
            str.append("alternateNames=" + alternateNames + ",");
        }
        str.append("latitude=" + latitude + ",");
        str.append("longitude=" + longitude + ",");
        str.append("countryCode=" + countryCode + ",");
        str.append("population=" + population + ",");
        str.append("elevation=" + elevation + ",");
        str.append("featureClass=" + featureClass + ",");
        str.append("featureCode=" + featureCode);
        return str.toString();
    }

    /**
     * @return the adminCode1
     */
    public String getAdminCode1() throws InsufficientStyleException {
        if (adminCode1 == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "adminCode1 not supported by style " + style.name());
        }
        return adminCode1;
    }

    /**
     * @param adminCode1
     *            the adminCode1 to set
     */
    public void setAdminCode1(String adminCode1) {
        this.adminCode1 = adminCode1;
    }

    /**
     * @return the adminCode2
     */
    public String getAdminCode2() throws InsufficientStyleException {
        if (adminCode2 == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "adminCode2 not supported by style " + style.name());
        }
        return adminCode2;
    }

    /**
     * @param adminCode2
     *            the adminCode2 to set
     */
    public void setAdminCode2(String adminCode2) {
        this.adminCode2 = adminCode2;
    }

    /**
     * @return the adminCode3
     */
    public String getAdminCode3() throws InsufficientStyleException {
        if (adminCode3 == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "adminCode3 not supported by style " + style.name());
        }
        return adminCode3;
    }

    /**
     * @param adminCode3
     *            the adminCode3 to set
     */
    public void setAdminCode3(String adminCode3) {
        this.adminCode3 = adminCode3;
    }

    /**
     * @return the adminCode4
     */
    public String getAdminCode4() throws InsufficientStyleException {
        if (adminCode4 == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "adminCode4 not supported by style " + style.name());
        }
        return adminCode4;
    }

    /**
     * @param adminCode4
     *            the adminCode4 to set
     */
    public void setAdminCode4(String adminCode4) {
        this.adminCode4 = adminCode4;
    }

    /**
     * @return the timezone
     */
    public Timezone getTimezone() throws InsufficientStyleException {
        if (timezone == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "alternateNames not supported by style " + style.name());
        }
        return timezone;
    }

    /**
     * @param timezone
     *            the timezone to set
     */
    public void setTimezone(Timezone timezone) {
        this.timezone = timezone;
    }

    /**
     * @return the adminName1
     */
    public String getAdminName1() throws InsufficientStyleException {
        if (adminName1 == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "adminName1 not supported by style " + style.name());
        }
        return adminName1;
    }

    /**
     * @param adminName1
     *            the adminName1 to set
     */
    public void setAdminName1(String adminName1) {
        this.adminName1 = adminName1;
    }

    /**
     * @return the adminName2
     */
    public String getAdminName2() throws InsufficientStyleException {
        if (adminName2 == null && style != null
                && Style.LONG.compareTo(style) > 0) {
            throw new InsufficientStyleException(
                    "adminName2 not supported by style " + style.name());
        }
        return adminName2;
    }

    /**
     * @param adminName2
     *            the adminName2 to set
     */
    public void setAdminName2(String adminName2) {
        this.adminName2 = adminName2;
    }

    /**
     * @return the style used when calling the web service that returned this
     *         toponym.
     */
    public Style getStyle() {
        return style;
    }

    /**
     * @param style
     *            the style to set
     */
    public void setStyle(Style style) {
        this.style = style;
    }

}
