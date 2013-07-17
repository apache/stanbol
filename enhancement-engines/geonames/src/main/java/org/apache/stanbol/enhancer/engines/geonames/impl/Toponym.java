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
package org.apache.stanbol.enhancer.engines.geonames.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Toponym {

    /**
     * JSON property names  keys used for Toponyms by geonames.org
     * (may not be complete)
     *
     * @author Rupert Westenthaler
     */
    enum ToponymProperty {
        geonameId,
        alternateNames,
        adminCode1,
        adminCode2,
        adminCode3,
        adminCode4,
        adminName1,
        adminName2,
        adminName3,
        adminName4,
        timezone,
        /**
         * The name determined from the alternate names based on the parsed
         * language in the request
         */
        name,
        /**
         * This is the official name (preferred label
         */
        toponymName,
        population,
        lat,
        lng,
        countryCode,
        countryName,
        score,
        fcode,
        fcodeNmae,
        fcl,
        fclName,
        elevation
    }

    private JSONObject data;

    public Toponym(JSONObject jsonData) {
        if (jsonData == null) {
            throw new NullPointerException("The parsed JSON object MUST NOT be NULL");
        }
        this.data = jsonData;
    }

    /**
     * @return the ISO 3166-1-alpha-2 countryCode.
     */
    public String getCountryCode() {
        try {
            return data.getString(ToponymProperty.countryCode.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.countryCode, data));
        }
    }


    /**
     * @return the elevation in meter.
     */
    public Integer getElevation() {
        try {
            if (data.has(ToponymProperty.elevation.name())) {
                return data.getInt(ToponymProperty.elevation.name());
            } else {
                return null;
            }
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.elevation, data));
        }
    }

    /**
     * the feature class {@link FeatureClass}
     *
     * @return the featureClass.
     *
     * @see <a href="http://www.geonames.org/export/codes.html">GeoNames Feature
     *      Codes</a>
     */
    public FeatureClass getFeatureClass() {
        try {
            String fc = data.getString(ToponymProperty.fcl.name());
            return fc == null ? null : FeatureClass.valueOf(fc);
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.fcl, data));
        }
    }

    /**
     * @return the featureCode.
     *
     * @see <a href="http://www.geonames.org/export/codes.html">GeoNames Feature
     *      Codes</a>
     */
    public String getFeatureCode() {
        try {
            return data.getString(ToponymProperty.fcode.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.fcode, data));
        }
    }

    /**
     * latitude in decimal degrees (wgs84)
     *
     * @return the latitude.
     */
    public double getLatitude() {
        try {
            return data.getDouble(ToponymProperty.lat.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.lat, data));
        }
    }

    /**
     * longitude in decimal degrees (wgs84)
     *
     * @return the longitude.
     */
    public double getLongitude() {
        try {
            return data.getDouble(ToponymProperty.lng.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.lng, data));
        }
    }

    /**
     * @return the name.
     */
    public String getName() {
        try {
            return data.getString(ToponymProperty.name.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.name, data));
        }
    }

    /**
     * @return the population.
     */
    public Long getPopulation() {
        try {
            if (data.has(ToponymProperty.population.name())) {
                return data.getLong(ToponymProperty.population.name());
            } else {
                return null;
            }
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.population, data));
        }
    }

    /**
     * @return the geoNameId.
     */
    public int getGeoNameId() {
        try {
            return data.getInt(ToponymProperty.geonameId.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.geonameId, data));
        }
    }

    /**
     * @return the featureClassName.
     */
    public String getFeatureClassName() {
        try {
            return data.getString(ToponymProperty.fclName.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.fclName, data));
        }
    }

    /**
     * @return the featureCodeName.
     */
    public String getFeatureCodeName() {
        try {
            return data.getString(ToponymProperty.fcodeNmae.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.fcodeNmae, data));
        }
    }

    /**
     * @return the countryName.
     */
    public String getCountryName() {
        try {
            return data.getString(ToponymProperty.countryName.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.countryName, data));
        }
    }

    /**
     * alternate names of this place as a list of string arrays with two entries
     * the first entry is the label and the second represents the language
     *
     * @return the alternateNames as comma separated list
     */
    public List<String[]> getAlternateNames() {
        try {
            if (data.has(ToponymProperty.alternateNames.name())) {
                List<String[]> parsedNames = new ArrayList<String[]>();
                JSONArray altNames = data.getJSONArray(ToponymProperty.alternateNames.name());
                for (int i = 0; i < altNames.length(); i++) {
                    JSONObject altName = altNames.getJSONObject(i);
                    if (altName.has("name")) {
                        parsedNames.add(new String[]{
                                altName.getString("name"),
                                altName.has("lang") ? altName.getString("lang") : null
                        });
                    } // else ignore alternate names without a name
                }
                return parsedNames;
            } else {
                return Collections.emptyList();
            }
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.alternateNames, data));
        }

    }

    public String toString() {
        return data.toString();
    }

    /**
     * @return the adminCode1
     */
    public String getAdminCode1() {
        try {
            return data.getString(ToponymProperty.adminCode1.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.adminCode1, data));
        }
    }

    /**
     * @return the adminCode2
     */
    public String getAdminCode2() {
        try {
            return data.getString(ToponymProperty.adminCode2.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.adminCode2, data));
        }
    }

    /**
     * @return the adminCode3
     */
    public String getAdminCode3() {
        try {
            return data.getString(ToponymProperty.adminCode3.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.adminCode3, data));
        }
    }

    /**
     * @return the adminCode4
     */
    public String getAdminCode4() {
        try {
            return data.getString(ToponymProperty.adminCode4.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.adminCode4, data));
        }
    }

    /**
     * The time zone is a complex Object encoded like
     * <code><pre>
     * timezone: {
     *     dstOffset: -5
     *     gmtOffset: -6
     *     timeZoneId: "America/Chicago"
     * }
     * </pre></code>
     * This mehtod does not further parse this data.
     *
     * @return the {@link JSONObject} with the time zone information
     */
    public JSONObject getTimezone() {
        try {
            return data.getJSONObject(ToponymProperty.timezone.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.timezone, data));
        }
    }

    /**
     * @return the adminName1
     */
    public String getAdminName1() {
        try {
            return data.getString(ToponymProperty.adminName1.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.adminName1, data));
        }
    }

    /**
     * @return the adminName2
     */
    public String getAdminName2() {
        try {
            return data.getString(ToponymProperty.adminName2.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.adminName2, data));
        }
    }

    /**
     * @return the adminName3
     */
    public String getAdminName3() {
        try {
            return data.getString(ToponymProperty.adminName3.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.adminName3, data));
        }
    }

    /**
     * @return the adminName4
     */
    public String getAdminName4() {
        try {
            return data.getString(ToponymProperty.adminName4.name());
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.adminName4, data));
        }
    }

    public Double getScore() {
        try {
            if (data.has(ToponymProperty.score.name())) {
                return data.getDouble(ToponymProperty.score.name());
            } else {
                return null;
            }
        } catch (JSONException e) {
            throw new IllegalStateException(String.format("Unable to parse %s form %s",
                    ToponymProperty.score, data));
        }
    }

}
