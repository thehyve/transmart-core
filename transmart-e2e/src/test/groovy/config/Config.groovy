package config

class Config {
    //Constants
    public static final String BASE_URL = 'http://localhost:8080/'
//    public static final String BASE_URL = 'http://transmart-pro-dev.thehyve.net/'
    public static final String BAD_USERNAME = 'bad username'
    public static final String BAD_PASSWORD = 'bad password'
    public static final String DEFAULT_USERNAME = 'test-public-user-1'
    public static final String DEFAULT_PASSWORD = 'test-public-user-1'
    public static final String UNRESTRICTED_USERNAME = 'test-public-user-2'
    public static final String UNRESTRICTED_PASSWORD = 'test-public-user-2'
    public static final String ADMIN_USERNAME = 'admin'
    public static final String ADMIN_PASSWORD = 'admin'

    public static final String PATH_OBSERVATIONS = "v2/observations"
    public static final String PATH_HIGH_DIM = "v2/high_dim"
    public static final String PATH_AGGREGATE = "v2/observations/aggregate"
    public static final String PATH_PATIENTS = "v2/patients"
    public static final String PATH_TREE_NODES = "v2/tree_nodes"
    public static final String PATH_PATIENT_SET = "v2/patient_sets"
    public static final String REGEXDATE = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"

    //study ids
    public static final String EHR_ID = 'EHR'
    public static final String CLINICAL_TRIAL_ID = 'CLINICAL_TRIAL'
    public static final String CATEGORICAL_VALUES_ID = 'CATEGORICAL_VALUES'
    public static final String TUMOR_NORMAL_SAMPLES_ID = 'TUMOR_NORMAL_SAMPLES'
    public static final String SHARED_CONCEPTS_A_ID = 'SHARED_CONCEPTS_STUDY_A'
    public static final String SHARED_CONCEPTS_B_ID = 'SHARED_CONCEPTS_STUDY_B'
    public static final String SHARED_CONCEPTS_RESTRICTED_ID = 'SHARED_CONCEPTS_STUDY_C_PRIV'

    //settings
    public static final boolean OAUTH_NEEDED = true
    public static final boolean DEBUG = true
    public static final boolean SUPPRESS_KNOWN_BUGS = true
    public static final boolean SUPPRESS_UNIMPLEMENTED = true

    //test studies loaded
    public static final boolean EHR_LOADED = true
    public static final boolean CLINICAL_TRIAL_LOADED = true
    public static final boolean CATEGORICAL_VALUES_LOADED = true
    public static final boolean TUMOR_NORMAL_SAMPLES_LOADED = true
    public static final boolean SHARED_CONCEPTS_LOADED = true
    public static final boolean SHARED_CONCEPTS_RESTRICTED_LOADED = true
}
