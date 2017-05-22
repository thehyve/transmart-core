Clinical Data File
================

The clinical data file contains the low-dimensional observations of each patient. The file name and columns are referenced from the [column mapping file](column-mapping.md). At least one clinical data file is needed for upload to tranSMART.

File format
------------

The basic structure of a clinical data file is patients on the rows and variables on the columns. In case the patient-variable combinations are all unique there will be no more than one row for each patient:

| Subject_id | Gender | Treatment arm |
|----------|--------|---------------|
| patient1 | Male   | A             |
| patient2 | Female | B             |

### Observation dates
When observations are linked to a specific date or time, the `start date` and optionally `end date` columns can be added. All observations present in a row with an observation date will be linked to that absolute point in time. Therefore one patient can have data spanning multiple rows:

| Subjects | Start date | End date   | Gender | Treatment arm | BMI  |
|----------|------------|------------|--------|---------------|------|
| patient1 |            |            | Male   | A             |      |
| patient1 | 2016-03-18 | 2016-03-18 |        |               | 22.7 |
| patient2 |            |            | Female | B             |      |
| patient2 | 2016-03-24 | 2016-03-24 |        |               | 20.9 |
