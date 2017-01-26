# Generate documentation with Schema Spy

You could use [Schema Spy](http://schemaspy.sourceforge.net/) to generate database documentation from the database.
To get pictures you have to install [Graphvis](http://www.graphviz.org/Download_macos.php).

Below is an examples on how to run Spider Spy for different databases:

    make -C db-doc/ generate_postgresql

    make -C db-doc/ generate_oracle

# Database design
TranSMART is based on the i2b2 data schema, which uses a star-schema design to store
observation data. Observations have several dimensions, like patient and concept.
These entities are represented in separate dimension tables and referred to in the observation table. E.g., an observation of blood pressure for patient P would be stored in the observation table with a reference to the concept for blood pressure and a reference to the record for patient P in the patient dimension. The value of the pressure reading is stored as numerical value in the observation record.

The observations have timestamps (start date is mandatory, end date is optional) and there can be many observations for the same combination of concept and patient: either by having a different start date, or by have a different instance number.

At the database level we use the i2b2 model for storing multiple values for a single patient-concept pair. This is the same for multiple samples, longitudinal and EHR data.

- In the case of multiple (unstructured) samples the instance_num column will be used to differentiate the samples. Additional data on samples (e.g. the tissue type) can be stored in modifiers.
- For longitudinal and EHR data the storage will be similar, but the different observations will differ in at least one of the encounter_num, trial_visit (a new dimension, see below for details), or the start_date.

Patient, concept and trial_visit are obligatory.
If per-patient visits are not available (e.g. in a regular clinical trial) the database stores a 0 value for encounter_num indicating that there is no visit.
If no across-patient visits are available (eg in EHR data) all observations from a study can be linked to the same dummy trial_visit. This trial_visit is still used to connect the observations to a study.

The Start_Date (End_Date) and Instance_Num can be marked as empty when they are not available. Only the dates (timestamps) from observations will be queryable, not those from trial_visit. Value, unit and label from trial_visit can be queried.

Common examples:

- A study with tumor and normal samples without time series.
    - For each concept there will be multiple observations for the same patient. They will be differentiated by their Instance_Num.
    - Each observation will store if it is from a tumor or not in a modifier.
    - The Start_Date (and End_Date) for the observation will be empty.
    - All observations will be linked to the same trial_visit, which will link to the study.
- A clinical trial with multiple timepoints (Baseline, Week 1, Week 2) without multiple samples and without date information available for observations and visits.
    - For each concept there will be multiple observations for the same patient. They will be differentiated by their trial_visit.
    - All observations will be linked to one of the available trial_visits, which will link to the study. Each trial_visit has a Label (Baseline), a Unit (Days) and a Value (0, 7 and 14).
    - The Start_Date (and End_Date) for the observation will be empty and the Instance_Num will be set to 1 (the first instance number).
    - All observations from a patient will be linked to the same visit. The Start_Date (and End_Date) for the visit will be empty.
- An EHR dataset with observation and visit timestamps and samples.
    - For each concept there will be multiple observations for the same patient. They will be differentiated by their observation Start_Date, visit and Instance_Num.
    - The Start_Date (and End_Date) for the observation will be set to a timestamp including date and time for the observation. The Instance_Num will be set starting from 1 for multiple samples on the same observation Start_Date and visit.
    - The observations from a patient will be linked one visit per hospital visit. The Start_Date (and End_Date) for the visit will be set to a timestamp including time and date for the hospital visit.
    - All observations will be linked to the same trial_visit, which will link to the study.

## Relevant tables

|   Notation   |      Meaning                  |
|------|------------------------|
| PK   | Primary Key            |
| FK   | Foreign key            |
| NO   | Nullable default = yes |
| Auto | Auto increment         |

### i2b2demodata.observation_fact
| Column           |           | 16.2   | i2b2 1.7 | 17.1       | Comments                                                                                                                                                                                                                      | 
|------------------|-----------|--------|----------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| 
| encounter_num    | numeric   |        | PK, NO   | PK, NO     | Refers to encounter_num in visit_dimension.                                                                                                                                                                                   | 
| patient_num      | numeric   | PK, NO | PK, NO   | PK, NO     | Refers to patient_num in patient_dimension.                                                                                                                                                                                   | 
| concept_cd       | varchar   | PK, NO | PK, NO   | PK, NO     | Refers to concept_cd in concept_dimension.                                                                                                                                                                                    | 
| provider_id      | varchar   | PK, NO | PK, NO   | PK, NO     | Refers to provider_id in provider_dimension.                                                                                                                                                                                  | 
| start_date       | timestamp |        | PK, NO   | PK, NO     | Starting date-time of the observation. Default: 0001-01-01 00:00:00.                                                                                                                                                          | 
| modifier_cd      | varchar   | PK, NO | PK, NO   | PK, NO     | Refers to modifier_cd in modifier_dimension. Default: @. Highdim values: [TRANSMART:HIGHDIM:GENE EXPRESSION, TRANSMART:HIGHDIM:RNASEQ_TRANSCRIPT], original variable: [TRANSMART:ORIGINAL_VARIABLE], sample type: [TNS:SMPL]. | 
| instance_num     | numeric   |        | PK, NO   | PK, NO     | Default: 1.                                                                                                                                                                                                                   | 
| trial_visit_num  | numeric   | NA     | NA       |            | Refers to the new trial_visit dimension. Is not part of the primary key to make the primary key of observation_fact identical with that used by i2b2.                                                                         | 
| valtype_cd       | varchar   |        |          |            | Either T for string values or N for numeric values.                                                                                                                                                                           | 
| tval_char        | varchar   |        |          |            | If valtype_cd is T, the observations text value. If valtype_cd is N, an i2b2 supported operator [E = Equals, NE = Not equal, L = Less than, LE = Less than or Equal to, G = Greater than, GE = Greater than or Equal to]      | 
| nval_num         | numeric   |        |          |            | Used in conjunction with valtype_cd = N to store a numerical value                                                                                                                                                            | 
| valueflag_cd     | varchar   |        |          |            |                                                                                                                                                                                                                               | 
| quantity_num     | numeric   |        |          |            |                                                                                                                                                                                                                               | 
| units_cd         | varchar   |        |          |            |                                                                                                                                                                                                                               | 
| end_date         | timestamp |        |          |            | The end date-time of the observation                                                                                                                                                                                          | 
| location_cd      | varchar   |        |          |            |                                                                                                                                                                                                                               | 
| observation_blob | text      |        |          |            |                                                                                                                                                                                                                               | 
| confidence_num   | numeric   |        |          |            |                                                                                                                                                                                                                               | 
| update_date      | timestamp |        |          |            |                                                                                                                                                                                                                               | 
| download_date    | timestamp |        |          |            |                                                                                                                                                                                                                               | 
| import_date      | timestamp |        |          |            |                                                                                                                                                                                                                               | 
| sourcesystem_cd  | varchar   |        |          | Deprecated | Deprecated. Is currently being ignored.                                                                                                                                                                                       | 
| upload_id        | numeric   |        |          |            |                                                                                                                                                                                                                               | 
| sample_cd        | varchar   |        | NA       | Deprecated | Deprecated. Refers to the sample_dimension table.                                                                                                                                                                             | 

### i2b2demodata.patient_dimension
| Column            |           | 16.2   | i2b2 1.7 | 17.1   | Comments                        | 
|-------------------|-----------|--------|----------|--------|---------------------------------| 
| patient_num       | numeric   | PK, NO | PK, NO   | PK, NO | Id of the patient.              | 
| vital_status_cd   | varchar   |        |          |        |                                 | 
| birth_date        | timestamp |        |          |        |                                 | 
| death_date        | timestamp |        |          |        |                                 | 
| sex_cd            | varchar   |        |          |        | One of [male, female, unknown]. | 
| age_in_years_num  | numeric   |        |          |        |                                 | 
| language_cd       | varchar   |        |          |        |                                 | 
| race_cd           | varchar   |        |          |        |                                 | 
| marital_status_cd | varchar   |        |          |        |                                 | 
| religion_cd       | varchar   |        |          |        |                                 | 
| zip_cd            | varchar   |        |          |        |                                 | 
| statecityzip_path | varchar   |        |          |        |                                 | 
| income_cd         | varchar   |        |          |        |                                 | 
| patient_blob      | text      |        |          |        |                                 | 
| update_date       | timestamp |        |          |        |                                 | 
| download_date     | timestamp |        |          |        |                                 | 
| import_date       | timestamp |        |          |        |                                 | 
| sourcesystem_cd   | varchar   |        |          |        |                                 | 
| upload_id         | numeric   |        |          |        |                                 | 

### i2b2demodata.concept_dimension
| Column          |           | 16.2   | i2b2 1.7 | 17.1   | Comments                                                             | 
|-----------------|-----------|--------|----------|--------|----------------------------------------------------------------------| 
| concept_cd      | varchar   | NO     |          | NO     | The code that is used to refer to the concept from observation_fact. | 
| concept_path    | varchar   | PK, NO | PK, NO   | PK, NO | The path that uniquely identifies a concept.                         | 
| name_char       | varchar   |        |          |        | The name of the concept.                                             | 
| concept_blob    | text      |        |          |        |                                                                      | 
| update_date     | timestamp |        |          |        |                                                                      | 
| download_date   | timestamp |        |          |        |                                                                      | 
| import_date     | timestamp |        |          |        |                                                                      | 
| sourcesystem_cd | varchar   |        |          |        |                                                                      | 
| upload_id       | int8      |        |          |        |                                                                      | 
| table_name      | varchar   |        | NA       |        |                                                                      | 

### i2b2demodata.modifier_dimension
| Column             |           | 16.2   | i2b2 1.7 | 17.1   | Comments                                                                                      | 
|--------------------|-----------|--------|----------|--------|-----------------------------------------------------------------------------------------------| 
| modifier_path      | varchar   | PK, NO | PK, NO   | PK, NO | The path that uniquely identifies a modifier.                                                 | 
| modifier_cd        | varchar   |        |          |        | The code that is used to refer to the modifier from obervation_fact. However, it is nullable. | 
| name_char          | varchar   |        |          |        | The name of the modifier.                                                                     | 
| modifier_blob      | text      |        |          |        |                                                                                               | 
| update_date        | timestamp |        |          |        |                                                                                               | 
| download_date      | timestamp |        |          |        |                                                                                               | 
| import_date        | timestamp |        |          |        |                                                                                               | 
| sourcesystem_cd    | varchar   |        |          |        |                                                                                               | 
| upload_id          | int8      |        |          |        |                                                                                               | 
| modifier_level     | int8      |        | NA       |        |                                                                                               | 
| modifier_node_type | varchar   |        | NA       |        |                                                                                               | 

### i2b2demodata.provider_dimension
| Column          |           | 16.2   | i2b2 1.7 | 17.1   | Comments                           | 
|-----------------|-----------|--------|----------|--------|------------------------------------| 
| provider_id     | varchar   | PK, NO | PK, NO   | PK, NO |                                    | 
| provider_path   | varchar   | PK, NO | PK, NO   | PK, NO | A path that identifies a provider. | 
| name_char       | varchar   |        |          |        | The name of the provider.          | 
| provider_blob   | text      |        |          |        |                                    | 
| update_date     | timestamp |        |          |        |                                    | 
| download_date   | timestamp |        |          |        |                                    | 
| import_date     | timestamp |        |          |        |                                    | 
| sourcesystem_cd | varchar   |        |          |        |                                    | 
| upload_id       | numeric   |        |          |        |                                    | 

### i2b2demodata.visit_dimension
| Column           |           | 16.2   | i2b2 1.7 | 17.1   | Comments                                                                      | 
|------------------|-----------|--------|----------|--------|-------------------------------------------------------------------------------| 
| encounter_num    | numeric   | PK, NO | PK, NO   | PK, NO | Id of the visit. Referred to by the encounter_num column of observation_fact. | 
| patient_num      | numeric   | PK, NO | PK, NO   | PK, NO | Id linking to patient_num in the patient_dimension.                           | 
| active_status_cd | varchar   |        |          |        |                                                                               | 
| start_date       | timestamp |        |          |        | Start date and time of the visit.                                             | 
| end_date         | timestamp |        |          |        | End date and time of the visit.                                               | 
| inout_cd         | varchar   |        |          |        |                                                                               | 
| location_cd      | varchar   |        |          |        |                                                                               | 
| location_path    | varchar   |        |          |        |                                                                               | 
| length_of_stay   | numeric   |        |          |        |                                                                               | 
| visit_blob       | text      |        |          |        |                                                                               | 
| update_date      | timestamp |        |          |        |                                                                               | 
| download_date    | timestamp |        |          |        |                                                                               | 
| import_date      | timestamp |        |          |        |                                                                               | 
| sourcesystem_cd  | varchar   |        |          |        |                                                                               | 
| upload_id        | numeric   |        |          |        |                                                                               | 

### i2b2demodata.study
| Column            |         | 16.2 | i2b2 1.7 | 17.1 | Comments                                                               | 
|-------------------|---------|------|----------|------|------------------------------------------------------------------------| 
| study_num         | numeric | NA   | NA       | Auto |                                                                        | 
| bio_experiment_id | int8    | NA   | NA       | FK   | Foreign key: bio_experiment_id in bio_experiment.                      | 
| study_id          | varchar | NA   | NA       | NO   | E.g., GSE8581.                                                         | 
| secure_obj_token  | varchar | NA   | NA       | NO   | Token needed for access to the study. E.g., ‘PUBLIC’ or ‘EXP:GSE8581’. | 

### i2b2demodata.trial_visit_dimension
| Column           |         | 16.2 | i2b2 1.7 | 17.1 | Comments                                                             | 
|------------------|---------|------|----------|------|----------------------------------------------------------------------| 
| trial_visit_num  | numeric | NA   | NA       | Auto |                                                                      | 
| study_num        | numeric | NA   | NA       | FK   | Foreign key to study_num in study.                                   | 
| rel_time_unit_cd | varchar | NA   | NA       |      | The unit in which rel_time_num is expressed. E.g., Week, Day, Visit. | 
| rel_time_num     | numeric | NA   | NA       |      | E.g., 1 (for Week 1).                                                | 
| rel_time_label   | varchar | NA   | NA       |      | Descriptive name. E.g., Baseline, Week 1.                            | 

### i2b2demodata.storage_system
| Column                  |         | 16.2 | i2b2 1.7 | 17.1   |
|-------------------------|---------|------|----------|--------|
| id                      | int4    | NA   | NA       | PK, NO |
| name                    | varchar | NA   | NA       |        |
| system_type             | varchar | NA   | NA       |        |
| url                     | varchar | NA   | NA       |        |
| system_version          | varchar | NA   | NA       |        |
| single_file_collections | bool    | NA   | NA       |        |

### i2b2demodata.linked_file_collection
| Column           |         | 16.2 | i2b2 1.7 | 17.1   |
|------------------|---------|------|----------|--------|
| id               | int4    | NA   | NA       | PK, NO |
| name             | varchar | NA   | NA       |        |
| study_id         | int4    | NA   | NA       | NO     |
| source_system_id | int4    | NA   | NA       | NO     |
| uuid             | varchar | NA   | NA       |        |

### i2b2demodata.supported_workflow
| Column               |         | 16.2 | i2b2 1.7 | 17.1   |
|----------------------|---------|------|----------|--------|
| id                   | int4    | NA   | NA       | PK, NO |
| name                 | varchar | NA   | NA       |        |
| description          | varchar | NA   | NA       |        |
| uuid                 | varchar | NA   | NA       | NO     |
| arvados_instance_url | varchar | NA   | NA       |        |
| arvados_version      | varchar | NA   | NA       |        |
| default_params       | text    | NA   | NA       |        |

### i2b2metadata.i2b2
| Column             |           | 16.2 | i2b2 1.7 | 17.1 |
|--------------------|-----------|------|----------|------|
| c_hlevel           | int4      | NO   | NO       | NO   |
| c_fullname         | varchar   | NO   | NO       | NO   |
| c_name             | varchar   | NO   | NO       | NO   |
| c_synonym_cd       | bpchar    | NO   | NO       | NO   |
| c_visualattributes | bpchar    | NO   | NO       | NO   |
| c_totalnum         | int4      |      |          |      |
| c_basecode         | varchar   |      |          |      |
| c_metadataxml      | text      |      |          |      |
| c_facttablecolumn  | varchar   | NO   | NO       | NO   |
| c_tablename        | varchar   | NO   | NO       | NO   |
| c_columnname       | varchar   | NO   | NO       | NO   |
| c_columndatatype   | varchar   | NO   | NO       | NO   |
| c_operator         | varchar   | NO   | NO       | NO   |
| c_dimcode          | varchar   | NO   | NO       | NO   |
| c_comment          | text      |      |          |      |
| c_tooltip          | varchar   |      |          |      |
| m_applied_path     | varchar   | NO   | NO       | NO   |
| update_date        | timestamp | NO   | NO       | NO   |
| download_date      | timestamp |      |          |      |
| import_date        | timestamp |      |          |      |
| sourcesystem_cd    | varchar   |      |          |      |
| valuetype_cd       | varchar   |      |          |      |
| m_exclusion_cd     | varchar   |      |          |      |
| c_path             | varchar   |      |          |      |
| c_symbol           | varchar   |      |          |      |
| i2b2_id            |           |      | NA       |      |


### i2b2metadata.i2b2_secure
| Column             |           | 16.2 | i2b2 1.7 | 17.1 |
|--------------------|-----------|------|----------|------|
| c_hlevel           | numeric   |      | NA       |      |
| c_fullname         | varchar   | NO   | NA       | NO   |
| c_name             | varchar   |      | NA       |      |
| c_synonym_cd       | bpchar    |      | NA       |      |
| c_visualattributes | bpchar    |      | NA       |      |
| c_totalnum         | numeric   |      | NA       |      |
| c_basecode         | varchar   |      | NA       |      |
| c_metadataxml      | text      |      | NA       |      |
| c_facttablecolumn  | varchar   |      | NA       |      |
| c_tablename        | varchar   |      | NA       |      |
| c_columnname       | varchar   |      | NA       |      |
| c_columndatatype   | varchar   |      | NA       |      |
| c_operator         | varchar   |      | NA       |      |
| c_dimcode          | varchar   |      | NA       |      |
| c_comment          | text      |      | NA       |      |
| c_tooltip          | varchar   |      | NA       |      |
| m_applied_path     | varchar   |      | NA       |      |
| update_date        | timestamp |      | NA       |      |
| download_date      | timestamp |      | NA       |      |
| import_date        | timestamp |      | NA       |      |
| sourcesystem_cd    | varchar   |      | NA       |      |
| valuetype_cd       | varchar   |      | NA       |      |
| m_exclusion_cd     | varchar   |      | NA       |      |
| c_path             | varchar   |      | NA       |      |
| c_symbol           | varchar   |      | NA       |      |
| i2b2_id            | numeric   |      | NA       |      |
| secure_obj_token   | varchar   |      | NA       |      |

### i2b2metadata.dimension_description
| Column        |         | 16.2 | i2b2 1.7 | 17.1 |
|---------------|---------|------|----------|------|
| id            | serial  | NA   | NA       | Auto |
| density       | varchar | NA   | NA       |      |
| modifier_code | varchar | NA   | NA       |      |
| value_type    | varchar | NA   | NA       |      |
| name          | varchar | NA   | NA       |      |
| packable      | varchar | NA   | NA       |      |
| size_cd       | varchar | NA   | NA       |      |

### i2b2metadata.study_dimension_descriptions
| Column                   |      | 16.2 | i2b2 1.7 | 17.1   |
|--------------------------|------|------|----------|--------|
| dimension_description_id | int8 | NA   | NA       | PK, NO |
| study_id                 | int8 | NA   | NA       | PK, NO |

### deapp.de_subject_microarray_data
| Column        |         | 16.2 | 17.1   |
|---------------|---------|------|--------|
| trial_name    | varchar |      |        |
| probeset_id   | int8    |      | PK, NO |
| assay_id      | int8    |      | PK, NO |
| patient_id    | int8    |      |        |
| sample_id     | int8    |      |        |
| subject_id    | varchar |      |        |
| raw_intensity | float8  |      |        |
| log_intensity | float8  |      |        |
| zscore        | float8  |      |        |
| new_raw       | float8  |      |        |
| new_log       | float8  |      |        |
| new_zscore    | float8  |      |        |
| trial_source  | varchar |      |        |
| partition_id  | numeric |      |        |

### deapp.de_subject_sample_mapping
| Column            |         | 16.2 | 17.1 |
|-------------------|---------|------|------|
| patient_id        | int8    |      |      |
| site_id           | varchar |      |      |
| subject_id        | varchar |      |      |
| subject_type      | varchar |      |      |
| concept_code      | varchar |      |      |
| assay_id          | int8    |      | NO   |
| patient_uid       | varchar |      |      |
| sample_type       | varchar |      |      |
| assay_uid         | varchar |      |      |
| trial_name        | varchar |      |      |
| timepoint         | varchar |      |      |
| timepoint_cd      | varchar |      |      |
| sample_type_cd    | varchar |      |      |
| tissue_type_cd    | varchar |      |      |
| platform          | varchar |      |      |
| platform_cd       | varchar |      |      |
| tissue_type       | varchar |      |      |
| data_uid          | varchar |      |      |
| gpl_id            | varchar |      |      |
| rbm_panel         | varchar |      |      |
| sample_id         | int8    |      |      | 
| sample_cd         | varchar |      |      |
| category_cd       | varchar |      |      | 
| source_cd         | varchar |      |      |
| omic_source_study | varchar |      |      |
| omic_patient_num  | int8    |      |      |
| omic_patient_id   | int8    |      |      |
| partition_id      | numeric |      |      |

### deapp.de_gpl_info
| Column             |           | 16.2   | 17.1   |
|--------------------|-----------|--------|--------|
| platform           | varchar   | PK, NO | PK, NO |
| title              | varchar   |        |        |
| organism           | varchar   |        |        |
| annotation_date    | timestamp |        |        |
| marker_type        | varchar   |        |        |
| release_nbr        | varchar   |        |        |
| genome_build       | varchar   |        |        |
| gene_annotation_id | varchar   |        |        |

### deapp.de_rnaseq_transcript_data
| Column                   |        | 16.2 | 17.1   |
|--------------------------|--------|------|--------|
| transcript_id            | int8   | NA   | PK, NO |
| assay_id                 | int8   | NA   | PK, NO |
| readcount                | int8   | NA   |        |
| normalized_readcount     | float8 | NA   |        |
| log_normalized_readcount | float8 | NA   |        |
| zscore                   | float8 | NA   |        |

### deapp.de_rnaseq_transcript_annot
| Column     |         | 16.2 | 17.1   |
|------------|---------|------|--------|
| id         | int8    | NA   | PK, NO | 
| gpl_id     | varchar | NA   | NO     |
| ref_id     | varchar | NA   | NO     | 
| chromosome | varchar | NA   |        |
| start_bp   | int8    | NA   |        | 
| end_bp     | int8    | NA   |        |
| transcript | varchar | NA   |        | 
