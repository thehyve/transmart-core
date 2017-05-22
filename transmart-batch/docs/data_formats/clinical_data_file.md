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

| Subject_id | Start date | End date   | Gender | Treatment arm | BMI  |
|----------|------------|------------|--------|---------------|------|
| patient1 |            |            | Male   | A             |      |
| patient1 | 2016-03-18 | 2016-03-18 |        |               | 22.7 |
| patient2 |            |            | Female | B             |      |
| patient2 | 2016-03-24 | 2016-03-24 |        |               | 20.9 |

### Trial visits
When one or multiple observations where acquired as part of a clinical trial, they can be mapped as such by adding a `Trial visit label` column. All observations in that row will be uploaded as part of the trial visit. The time unit and value of each trial visit can be provided in the [trial visit mapping file](trial-visit-mapping.md).

| Subject_id | Trial visit label | Gender | Treatment arm | BMI  | Heart rate |
|----------|-------------------|--------|---------------|------|------------|
| patient1 |                   | Male   | A             |      |            |
| patient1 | Baseline          |        |               | 22.7 | 87         |
| patient1 | Week 5            |        |               | 22.6 | 91         |
| patient2 |                   | Female | B             |      |            |
| patient2 | Baseline          |        |               | 20.9 | 82         |
| patient2 | Week 5            |        |               | 20.5 | 82         |


### Patient visits (not yet implemented)

**Note:** In the examples above, each variation on the basic structure of clinical data files is shown separately for clarity reasons. However, none of them are mutually exclusive. In principle, a patient's observation value can be parth of both a trial visit and a patient visit while having its own observation start and end date.
