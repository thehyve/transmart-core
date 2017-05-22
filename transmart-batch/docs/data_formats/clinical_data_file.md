Clinical Data File
================

The clinical data file contains the low-dimensional observations of each patient. The file name and columns are referenced from the [column mapping file](column-mapping.md). At least one clinical data file is needed for upload to tranSMART. Each data file must contain a column with the patient identifiers.

File format
------------

The basic structure of a clinical data file is patients on the rows and variables on the columns. In case the patient-variable combinations are all unique there will be no more than one row for each patient.

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
Observations derived from electronic health records can be uploaded as part of a patient visit by adding the `Patient visit` column. All observations in that row will be uploaded as part of the patient visit. A patient visit's start and end date can be provided in the [patient visit mapping file](patient-visit-mapping.md).

| Subject_id | Patient visit | Gender | Treatment arm | BMI  | Heart rate | Hb level |
|------------|---------------|--------|---------------|------|------------|----------|
| patient1   |               | Male   | A             |      |            |          |
| patient1   | 1             |        |               | 22.6 | 91         |          |
| patient2   |               | Female | B             |      |            |          |
| patient2   | 1             |        |               | 20.9 | 82         |          |
| patient2   | 2             |        |               |      | 69         | 142      |

### Replicate observations
By default, tranSMART expects unique combinations of subject ID, patient visit, trial visit, observation start date and variable. In case there are repeated measurements (i.e. not distinguishable by aforementioned aspects) that you wish to upload, this has to be made explicit by using the `instance number` column (see also [column mapping file](column-mapping.md)).

| Subject_id | Start date | Instance num | Gender | Treatment arm | Heart rate | Hb level |
|------------|------------|--------------|--------|---------------|------------|----------|
| patient1   |            |              | Male   | A             |            |          |
| patient1   | 2009-01-13 | 1            |        |               | 91         | 142      |
| patient1   | 2009-01-13 | 2            |        |               |            | 140      |

### Custom modifiers (not yet implemented)
Apart from the above mentioned methods of providing additional context to observation values, it is also possible to add your own modifier values to observations. This can be done by adding a column for each variable that requires modification. To which variable the modifier values should be applied is defined in your [column mapping file](column-mapping.md). In the example below, the *_Fasting (h)_* column could act as a modifier for the *_Hb level_* variable.

| Subject_id | Instance num | Gender | Treatment arm | Hb level | Fasting (h) |
|------------|--------------|--------|---------------|----------|-------------|
| patient1   |              | Male   | A             |          |             |
| patient1   | 1            |        |               | 142      | 3           |
| patient1   | 2            |        |               | 140      | 24          |

___

**Note:** In the examples above, each variation on the basic structure of clinical data files is shown separately for clarity reasons. However, none of them are mutually exclusive. In principle, a patient's observation value can be part of both a trial visit and patient visit, while having its own observation start and end date and any nubmer of custom modifiers.
