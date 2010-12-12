/*
 * Copyright 2006 Marc Wick, geonames.org
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
package org.geonames.utils;

import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;
import static java.lang.Math.toDegrees;

/**
 * compass bearing from the first point to the second point in degrees.
 *
 * @author Mark Thomas
 */
public class Bearing {

    /**
     * Returns the direction from the first point to the second point in
     * degrees. The direction is the clockwise angle between the magnetic north
     * and the direction from point1 to point2
     *
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    public static double calculateBearing(double lat1, double lng1,
            double lat2, double lng2) {
        final double x3 = toRadians(lat1);
        final double y3 = toRadians(lng1);
        final double x4 = toRadians(lat2);
        final double y4 = toRadians(lng2);
        double numerator = (sin(y3) * sin(x4 - x3));
        double denominator = ((sin(y4) * cos(y3)) - (cos(y4) * sin(y3) * cos(x4
                - x3)));
        double bearing = toDegrees(atan(numerator / denominator));
        if (denominator > 0) {
            bearing += 360D;
        } else if (denominator > 0) {
            bearing += 180;
        }
        return (bearing < 0 ? (bearing + 360D) : bearing);
    }
}
