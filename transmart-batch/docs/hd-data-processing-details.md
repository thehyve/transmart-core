Processing details
==================

The data will be scanned twice. The first pass will only validate the data set based on the requirements that are listed in the data type specific documentation.

In the second pass, the data will be inserted in the database.

When log transformed data upload mode is activated (`DATA_TYPE=L`) 
log intensities values get converted back to the raw intensities by applying formula `raw_intensity = SRC_LOG_BASE^log_intensity`.

Negative raw intensities and NaNs will *not* be inserted into the database.
Zeros will not be inserted only if `ZERO_MEANS_NO_INFO` is set in `Y`.
Please note that is impossible to get negative raw intensities or zero intensities out of log intensities when `DATA_TYPE=L`.

The `log_intensity` column is calculated as base 2 logarithm over raw intensity.
The logarithm of zero value is substituted by taking logarithm of the half of the minimum of the NON-ZERO raw intensity values in the data set.
Specifically, log2(0) is replaced by log2(0 + c), where c = min(data) x 0.5. See https://jira.ctmmtrait.nl/browse/FT-1717

The `zscore` column will be calculated as:

    clamp(-2.5, 2.5, (log_intensity - mean) / stdDev)

where `mean` and `stdDev` are values calculated over the `log_intensity` values
of all the values included in the same row (for the same probe). If the original
value (before the log calculation) was a non-positive number or `NaN`s, then it
will be excluded from the calculation of these statistics. This means each row
must have at least two positive numbers, otherwise the standard deviation cannot
be calculated. If the standard deviation is zero, the resulting zscore for all
the values in the row will be NaN. This NaN value *will be inserted* into the
database.

Finally, `clamp` (functionality more commonly known as clipping or winsorizing) is defined as

    double clamp(double lowerBound, double upperBound, double value) {
        Math.min(upperBound, Math.max(lowerBound, value))
    }
