package org.transmartproject.batch.stat

import static java.lang.Math.min
import static java.lang.Math.log

/**
 * Contains transmart particularities of statistical measurement calculations.
 */
class StatisticsCalculationUtils {

    static double clamp(double lowerBound, double upperBound, double value) {
        min(upperBound, Math.max(lowerBound, value))
    }

    /**
     * The zero value would be replaced by the half of the minimum of the NON-ZERO intensity values in the data.
     * Specifically, log2(0) is replaced by log2(0 + c), where c = min(data) x 0.5
     * see https://jira.ctmmtrait.nl/browse/FT-1717
     * @param value - value to convert. Raw value
     * @param minPosDataSetValue - minimal positive value across whole data set.
     * @return log(minPosDataSetValue / 2) if value = 0 and ordinal log of value otherwise
     */
    static double log(double value, double minPosDataSetValue) {
        if (value == 0) {
            log(minPosDataSetValue / 2)
        } else {
            log(value)
        }
    }

}
