High dimensional data parameters
--------------------------------

- `DATA_FILE` **(Mandatory)** Points to the HD data file. _Preferred over_ `DATA_FILE_PREFIX`. 
- `DATA_FILE_PREFIX` **(Deprecated)** 
- `DATA_TYPE` **(Mandatory)** The current state of your data values. Can be `R` (raw values) or `L` (log-transformed values).
- `LOG_BASE` _Default:_ `2`. In case you provide raw values, this is the base used for doing the log transformation. Currently only supports `2`.
- `SRC_LOG_BASE` **(Mandatory if `DATA_TYPE`=`L`)** The log base that was used for transforming the data to their current values. Has to be specified only with `DATA_TYPE=L`. This will be used to reconstruct the raw intensity values.
- `MAP_FILENAME` **(Mandatory)** Name of the [subject-sample mapping file](subject-sample-mapping.md).
- `ALLOW_MISSING_ANNOTATIONS` _Default:_ `N`. `Y` for yes, `N` for no. Whether the job should be allowed to continue when the data set doesn't provide data for all the annotations.
- `SKIP_UNMAPPED_DATA` _Default:_ `N`. If `Y`, it ignores data points of samples that are not present in the [subject-sample mapping file](subject-sample-mapping.md). Otherwise (`N`) gives an error for such data points.
- `ZERO_MEANS_NO_INFO` _Default:_ `N`. If `Y`, raw values that equal 0 will be filtered out. Otherwise (`N`), they will be inserted into the database.
    The flag applies to most of HD data types. It does not effect CNV (aCGH) data. For RNASeq read count data the check on zeros happens based on normalized read count.
