package config

class Config {
    //Constants
//    public static final String BASE_URL = 'http://localhost:8080/'
    public static final String BASE_URL = 'http://transmart-pro-test.thehyve.net/transmart/'
    public static final String BAD_USERNAME = 'bad username'
    public static final String BAD_PASSWORD = 'bad password'
    public static final String GOOD_USERNAME = 'admin'
    public static final String GOOD_PASSWORD = 'admin'
    public static final String ADMIN_USERNAME = ''
    public static final String ADMIN_PASSWORD = ''

    public static final String REGEXDATE = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"

    //study ids
    public static final String EHR_ID = 'EHR'
    public static final String CLINICAL_TRIAL_ID = 'CLINICAL_TRIAL'
    public static final String CATEGORICAL_VALUES_ID = 'CATEGORICAL_VALUES'
    public static final String TUMOR_NORMAL_SAMPLES_ID = 'TUMOR_NORMAL_SAMPLES'

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
}
