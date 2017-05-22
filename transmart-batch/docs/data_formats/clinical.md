Clinical Data
================

Clinical data is meant for all kind of measurements not falling into other
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
