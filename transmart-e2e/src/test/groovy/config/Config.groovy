package config

/**
 * Created by barteldklasens on 10/25/16.
 */
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

    //study ids
    public static final String WARD_EHR_ID = 'Ward-EHR'

    //settings
    public static final boolean OAUTHNEEDED = true
    public static final boolean DEBUG = true

    //test studies loaded
    public static final boolean WARD_CLINICALTRAIL_LOADED = true
    public static final boolean WARD_EHR_LOADED = true
}
