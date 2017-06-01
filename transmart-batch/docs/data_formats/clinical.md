Clinical Data
================

Clinical data is meant for all kinds of measurements not falling into other
categories. It can be data from questionnaires, physical body measurements or
socio-economic info about the patients.

The bare minimum that is needed to upload clinical data is a [data file](clinical_data_file.md) and a [column mapping file](column-mapping.md).

Parameters
------------
The parameters file should be named `clinical.params` and may contain:
- `COLUMN_MAP_FILE` **(Mandatory)** Points to the [column mapping file](column-mapping.md).
- `WORD_MAP_FILE` Points to the [word mapping file](word-mapping.md).
- `XTRIAL_FILE` Points to the [cross study concepts file](xtrial.md).
- `TAGS_FILE` Points to the [concepts tags file](tags.md). Alternatively this may be specified in your [tags parameter file](tags.md).
- `ONTOLOGY_MAP_FILE` Points to the [ontology mapping file](ontology-mapping.md).
- `TRIAL_VISIT_MAP_FILE` **(Not yet implemented)** Points to the [trial visit mapping file](trial-visit-mapping.md).
- `PATIENT_VISIT_MAP_FILE` **(Not yet implemented)** Points to the [patient visit mapping file](patient-visit-mapping.md).

Clinical data upload
------------
Uploading the clinical data is usually the first procedure when uploading a study to tranSMART:
- Place the `clinical.params` file into the `clinical` folder of your study, alongside all other low-dimensional files that should be part of this upload, as specified in your clinical parameters (see above).
- Run the upload pipeline from your transmart-batch folder:  
`./transmart-batch.sh -p ./studies/GSE8581/clinical/clinical.params`

#### Clinical data deletion
Clinical data observations can be deleted by running the [backout.params](backout.md).
