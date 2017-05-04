Word mapping
================

The word mapping is an optional part of the clinical data upload process. It can be used in case a codebook already exists, or simply to modify data values without touching the source data. Through the word mapping it is possible to assign a different value to a categorical variable for upload to tranSMART. The name of your word mapping file must be specified at the `WORD_MAP_FILE` parameter of your [clinical params file](clinical.md).

WORD_MAP_FILE format
------------

|Filename|Column Number|Original value|New value |
|--------|-------------|--------------|----------|
|data.txt|5            |m             |Male      |
|data.txt|5            |f             |Female    |
|data.txt|12           |0             |No        |
|data.txt|12           |1             |Yes       |
|data.txt|12           |-1            |Not measured|

Table, tab separated, txt file. Header must be present, but is not interpreted (i.e. column order is fixed).

- `Filename` **(Mandatory)** The name of the file that contains the source data that should be renamed.
- `Column Number` **(Mandatory)** Index of the column from the data file, starting at 1.
- `Original value` **(Mandatory)** A categorical value present in the source data that should be renamed.
- `New value` **(Mandatory)** New categorical value that should replace the original one.

**NOTE:** The word mapping information itself is not stored in tranSMART. Hence, only the new values that are provided will be stored and visible in the application.
