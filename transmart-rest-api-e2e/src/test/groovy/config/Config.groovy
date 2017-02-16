/* Copyright © 2017 The Hyve B.V. */
package config

class Config {
    //Constants
    //$ gradle -DbaseUrl=http://transmart-pro-test.thehyve.net/ test
    public static final String BASE_URL = System.getProperty('baseUrl') != null ? System.getProperty('baseUrl') : 'http://localhost:8080/'
    public static final String BAD_USERNAME = 'bad username'
    public static final String BAD_PASSWORD = 'bad password'
    public static final String DEFAULT_USERNAME = 'test-public-user-1'
    public static final String DEFAULT_PASSWORD = 'test-public-user-1'
    public static final String UNRESTRICTED_USERNAME = 'test-public-user-2'
    public static final String UNRESTRICTED_PASSWORD = 'test-public-user-2'
    public static final String ADMIN_USERNAME = 'admin'
    public static final String ADMIN_PASSWORD = 'admin'

    public static final String VERSIONS_PATH = '/versions'
    public static final String NON_EXISTING_API_VERSION = 'v0'

    public static final String V1_PATH_STUDIES = "v1/studies"
    public static final String V1_PATH_observations = "v1/observations"
    public static final String V1_PATH_PATIENT_SETS = "v1/patient_sets"

    public static final String PATH_OBSERVATIONS = "v2/observations"
    public static final String PATH_AGGREGATE = "v2/observations/aggregate"
    public static final String PATH_COUNTS = "v2/observations/count"
    public static final String PATH_SUPPORTED_FIELDS = "v2/supported_fields"
    public static final String PATH_PATIENTS = "v2/patients"
    public static final String PATH_TREE_NODES = "v2/tree_nodes"
    public static final String PATH_STUDIES = "v2/studies"
    public static final String PATH_PATIENT_SET = "v2/patient_sets"
    public static final String PATH_STORAGE = "v2/storage"
    public static final String PATH_FILES = "v2/files"
    public static final String PATH_ARVADOS_WORKFLOWS = "v2/arvados/workflows"

    //study ids
    public static final String ORACLE_1000_PATIENT_ID = 'ORACLE_1000_PATIENT'
    public static final String GSE8581_ID = 'GSE8581'
    public static final String CELL_LINE_ID = 'CELL-LINE'
    public static final String EHR_ID = 'EHR'
    public static final String EHR_HIGHDIM_ID = 'EHR_HIGHDIM'
    public static final String CLINICAL_TRIAL_ID = 'CLINICAL_TRIAL'
    public static final String CATEGORICAL_VALUES_ID = 'CATEGORICAL_VALUES'
    public static final String TUMOR_NORMAL_SAMPLES_ID = 'TUMOR_NORMAL_SAMPLES'
    public static final String SHARED_CONCEPTS_A_ID = 'SHARED_CONCEPTS_STUDY_A'
    public static final Long SHARED_CONCEPTS_A_DB_ID = -27L
    public static final String SHARED_CONCEPTS_B_ID = 'SHARED_CONCEPTS_STUDY_B'
    public static final Long SHARED_CONCEPTS_B_DB_ID = -28L
    public static final String SHARED_CONCEPTS_RESTRICTED_ID = 'SHARED_CONCEPTS_STUDY_C_PRIV'
    public static final Long SHARED_CONCEPTS_RESTRICTED_DB_ID = -29L
    public static final String RNASEQ_TRANSCRIPT_ID = 'RNASEQ_TRANSCRIPT'
    public static final String MIX_HD_ID = 'MIX_HD'

    //settings
    public static final boolean DB_MIGRATED = true
    public static final boolean OAUTH_NEEDED = true
    public static final boolean DEBUG = true
    public static final boolean SUPPRESS_KNOWN_BUGS = true
    public static final boolean SUPPRESS_UNIMPLEMENTED = true
    public static final boolean RUN_HUGE_TESTS = true

    //test studies loaded
    public static final boolean ORACLE_1000_PATIENT_LOADED = false
    public static final boolean GSE8581_LOADED = false
    public static final boolean CELL_LINE_LOADED = false
    public static final boolean EHR_LOADED = true
    public static final boolean EHR_HIGHDIM_LOADED = true
    public static final boolean CLINICAL_TRIAL_LOADED = true
    public static final boolean CATEGORICAL_VALUES_LOADED = true
    public static final boolean TUMOR_NORMAL_SAMPLES_LOADED = true
    public static final boolean SHARED_CONCEPTS_LOADED = true
    public static final boolean SHARED_CONCEPTS_RESTRICTED_LOADED = true
}
