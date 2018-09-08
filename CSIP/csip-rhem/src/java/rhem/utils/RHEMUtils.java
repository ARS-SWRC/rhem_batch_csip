/*
 * $Id$
 *
 * This file is part of the Cloud Services Integration Platform (CSIP),
 * a Model-as-a-Service framework, API, and application suite.
 *
 * 2012-2017, OMSLab, Colorado State University.
 *
 * OMSLab licenses this file to you under the MIT license.
 * See the LICENSE file in the project root for more information.
 */
package rhem.utils;

/**
 *
 * @author rumpal
 */
public class RHEMUtils {
    
    public static final String[] MONTH_NAMES_LIST = {"January", "February", "March", "April", "May",
            "June", "July", "August", "September", "October", "November", "December"};

    //Round the value of a float or double
    public static double roundValues(double value, int places) {
        double power = Math.pow(10, places);
        return (Math.round(value * power) / power);
    }

}
