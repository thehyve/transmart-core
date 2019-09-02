TEST DATA DESCRIPTION

### UPDATE 2019/07/25

# STUDIES

  Public studies
  ├── Multi-DWH Test               -> Study ID: MULTI-DWH-TEST-STUDY
  └── Ontology overlap test study -> Study ID: ONTOLOGY-OVERLAP-STUDY

  Tree structure: MULTI-DWH-TEST-STUDY

  Multi-DWH Test
  └── Simple variables
  |   ├── Sex
  |   ├── Age
  |   ├── Date of birth
  |   └── Favorite color
  ├── Ontology variables          -> Variables mapped to an ontology AND used only by this study (there are more ontology-mapped variables that are shared between studies, see ONTOLOGY-OVERLAP-STUDY to know which ones)
  |   └── Name
  ├── Observation date variables  -> Observations for these variables have a start_date
  |   └── BMI
  ├── Visit variables             -> Multiple observations for the same patient-variable combination, based on visit_dimension
  |   └── Height
  ├── Trial visit variables       -> Multiple observations for the same patient-variable combination, based on trial_visit_dimension
  |   └── Weight
  └── Modifier variables          -> Additional info added to observation via modifiers
      └── blood pressure

  Patients: P0,1,5,6,8,9

  Tree structure: ONTOLOGY-OVERLAP-STUDY

  Ontology overlap test study
  └── 1. Shared variables       -> Variables in this folder have observations in both studies (i.e. same concept_code, though name might differ)
  |   ├── Age
  |   ├── Sex
  |   ├── Birthdate
  |   ├── Height (ONT)          -> LINKED TO ONTOLOGY CODE (NOTE: observations are linked ONLY to trial_visit_dimension, NOT to visit_dimension as in the other study)
  |   └── Favorite color (ONT)  -> LINKED TO ONTOLOGY CODE
  └── 2. Private variables      -> Variables in this folder have observations only in this study
      ├── Shoe size (ONT)       -> LINKED TO ONTOLOGY CODE
      └── Favorite animal (ONT) -> LINKED TO ONTOLOGY CODE

  Patients: P2,3,4,7

# PEDIGREE DATA

  MULTI-DWH-TEST-STUDY:

    P1 is PARENT of P5 (and vice versa P5 is child of P1)
    P6 is the FUTURE SELF of P5 (and vice versa P5 is the PAST SELF of P6) - time traveling involved..
    P6 and P8 are SPOUSES; P8 happens to have a CLONE P9 (P8 is CLONED BY P9, P9 is a CLONE OF P8)
    P6 and P0 are DOPPELGANGERS

  ONTOLOGY-OVERLAP-STUDY:
    P2 and P3 are TWINS; P4 is a SIBLING (non-biological) to both of them
    P7 is very lonely.

  See tables:

    relation (NEW)
    relation_type (NEW)

# VISIT DIMENSION

  I created a visit for all patients in MULTI-DWH-TEST-STUDY (encounter_num = 0), plus another visit for a subset of them (encounter_num = 1);
  then associated all existing Height observations to encounter_num = 0, and created new Height observations for the subset, associated to encounter_num = 1.
  Height observations for patients in ONTOLOGY-OVERLAP-STUDY remained linked to the general trial visit dimension for that study (3), instead of visit_dimension.

  See tables:

    visit_dimension (NEW)
    encounter_mapping (NEW)
    observation_fact
    i2b2_secure                 -> changed Height path to Visit variables/Height (previously Simple variables)
    dimension_description       -> added visit to the listed dimensions
    study_dimension_description -> added index of visit (from previous table) to the ONTOLOGY-OVERLAP-STUDY

# CUSTOM ONTOLOGY

  I created an ontology that is shared among the two studies (with partial overlap) as follows:

    Custom ontologies
    └── Ontology Test
        ├── Bodily measures
        |   ├── Height (m)                      -> Height (BOTH STUDIES; NOTE: linked to visit_dimension only in MULTI-DWH-TEST-STUDY!)
        |   └── Shoe size (EU)                  -> Shoe size (only ONTOLOGY-OVERLAP-STUDY)
        └── Individual Preferences
            ├── Favorite visible light interval -> Favorite color (BOTH STUDIES)
            ├── Favorite animal                 -> Favorite animal (only ONTOLOGY-OVERLAP-STUDY)
            └── How I prefer to be called       -> Name (only MULTI-DWH-TEST-STUDY)

  All ontology nodes (folders and leaves, excluding top folder "Custom ontologies" but including "Ontology test") have corresponding concept codes in the concept_dimension
  and they are linked to these codes in the i2b2_secure table. However ONLY ontology leaves have corresponding observations in the observation_fact table.

  I also did some cleanup in the concept_dimension table:
  - replaced study variables concept codes with corresponding ontology codes
  - for those variables that don't map to an ontology code, replaced the randomly generated concept code with a more readable one
  - shortened all concept paths and made them study-independent (they shouldn't mimics study tree paths, which is especially confusing when concepts are used by multiple studies.. it just needs to be a unique string)
  Concept codes and concept paths in i2b2_secure and observation_fact where updated accordingly.

  See tables:

    concept_dimension           -> ontology (and other variable) codes
    i2b2_secure                 -> ontology (and study) tree structure
