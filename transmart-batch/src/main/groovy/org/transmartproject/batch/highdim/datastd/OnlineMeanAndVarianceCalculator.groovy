package org.transmartproject.batch.highdim.datastd

import com.google.common.base.MoreObjects
import groovy.transform.AutoClone
import groovy.transform.CompileStatic

/**
 * Computes the mean and variance online.
 * See {@url http://www.johndcook.com/blog/standard_deviation/}.
 */
@CompileStatic
@AutoClone
class OnlineMeanAndVarianceCalculator implements Serializable {
    private long n
    private double meanOld, meanNew, varOld, varNew

    private final static long serialVersionUID = 1L

    void reset() {
        n = 0
        meanOld = meanNew = varOld = varNew = 0d
    }

    void push(double x) {
        n++

        if (n == 1L) {
            meanOld = meanNew = x
            varOld = 0.0
        } else {
            meanNew = meanOld + (x - meanOld) / n
            varNew = varOld + (x - meanOld) * (x - meanNew)

            meanOld = meanNew
            varOld = varNew
        }
    }

    long getN() {
        n
    }

    double getMean() {
        n > 0 ? meanNew : 0
    }

    double getVariance() {
        (n > 1) ? varNew / (n - 1) : 0.0d
    }

    double getStandardDeviation() {
        Math.sqrt(variance)
    }


    @Override
    String toString() {
        MoreObjects.toStringHelper(this)
                .add("n", n)
                .add("mean", mean)
                .add("variance", variance)
                .toString()
    }
}
