Custom zscore upload
====================

You could choose to upload your custom z-scores by adding a column with `.zscore` siffix in the header name.

    <sample code>.zscore

e.g. `GSM210005.zscore`

**NOTE:** Once you introduce the zscore column for a sample, you have to declare zscore columns for all the rest sample columns in the data file.
Uploading zscores for part of the samples in the data file is not allowed, system will throw a validation error.
