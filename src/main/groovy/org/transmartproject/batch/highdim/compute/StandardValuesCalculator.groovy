package org.transmartproject.batch.highdim.compute

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import javax.annotation.PostConstruct

/**
 * Calculates log intensity and zscore.
 */
@Component
@JobScope
class StandardValuesCalculator {

    private static final double LOG_2 = Math.log(2d)

    /**
     * See {@link MeanAndVariancePromoter#CALCULATED_MEAN}.
     */
    @Value("#{jobExecutionContext['calculatedMean']}")
    Double mean

    /**
     * See {@link MeanAndVariancePromoter#CALCULATED_VARIANCE}.
     */
    @Value("#{jobExecutionContext['calculatedVariance']}")
    Double variance

    @Lazy
    Double stdDev = Math.sqrt(variance)

    @PostConstruct
    void init() {
        Assert.notNull(mean)
        Assert.notNull(variance)
    }

    private double clamp(double lowerBound, double upperBound, double value) {
        Math.min(upperBound, Math.max(lowerBound, value))
    }

    Map<String, Double> getValueTriplet(DataPoint dataPoint) {
        double logIntensity = Math.log(dataPoint.value) / LOG_2

        [
                raw_intensity: dataPoint.value,
                log_intensity: logIntensity,
                zscore:        clamp(-2.5d, 2.5d, (logIntensity - mean) / stdDev),
        ]
    }
}
