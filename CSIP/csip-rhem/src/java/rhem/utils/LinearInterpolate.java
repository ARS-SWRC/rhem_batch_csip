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
public class LinearInterpolate {

    public static double interpolate(double pointToEvaluate, double[] functionValuesX, double[] functionValuesY) {
        double result = 0;
        int index = findIntervalLeftBorderIndex(pointToEvaluate, functionValuesX);
        if (index == functionValuesX.length - 1) {
            index--;
        }
        result = linearInterpolation(pointToEvaluate, functionValuesX[index], functionValuesY[index],
                functionValuesX[index + 1], functionValuesY[index + 1]);
        return result;
    }

    public static int findIntervalLeftBorderIndex(double point, double[] intervals) {

        if (point < intervals[0]) {
            return 0;
        }
        if (point > intervals[intervals.length - 1]) {
            return intervals.length - 1;
        }

        int leftBorderIndex = 0;
        int indexOfNumberToCompare;
        int rightBorderIndex = intervals.length - 1;

        while ((rightBorderIndex - leftBorderIndex) != 1) {
            indexOfNumberToCompare = leftBorderIndex
                    + (int) Math.floor(((rightBorderIndex - leftBorderIndex) / 2));
            if (point >= intervals[indexOfNumberToCompare]) {
                leftBorderIndex = indexOfNumberToCompare;
            } else {
                rightBorderIndex = indexOfNumberToCompare;
            }
        }
        return leftBorderIndex;
    }

    public static double linearInterpolation(double x, double x0, double y0, double x1, double y1) {
        double a = (y1 - y0) / (x1 - x0);
        double b = -a * x0 + y0;
        return a * x + b;
    }
}
