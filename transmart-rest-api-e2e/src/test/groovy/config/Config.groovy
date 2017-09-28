/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package config

import base.ContentTypeFor
import base.TestContext
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.FromServer

import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.HttpVerb.GET

class Config {
    //$ gradle -DbaseUrl=http://transmart-pro-test.thehyve.net/ test
    public static
    final String BASE_URL = System.getProperty('baseUrl') != null ? System.getProperty('baseUrl') : 'http://localhost:8080/'
//    public static final AuthAdapter authAdapter = new AuthAdapterOauth()

    //Configure the default TestContext. This is shared between all tests unless it is replaced by a testClass
    public static final TestContext testContext = new TestContext().setHttpBuilder(configure {
        request.uri = BASE_URL
        // custom parsers
        response.parser(ContentTypeFor.PROTOBUF) { ChainedHttpConfig cfg, FromServer fs ->
            ProtobufHelper.parse(fs.inputStream)
        }
        // custom interceptor
        execution.interceptor(GET) { cfg, fx ->
            // set default type for PATH_OBSERVATIONS
            if (cfg.request.uri.path == PATH_OBSERVATIONS && !cfg.request.uri.query.type) {
                cfg.request.uri.query.type = 'clinical'
            }
            fx.apply(cfg)
        }
    }).setAuthAdapter(new OauthAdapter())

    public static final String TEMP_DIRECTORY = '/tmp'

    public static final String DEFAULT_USER = 'test-public-user-1'
    public static final String UNRESTRICTED_USER = 'test-public-user-2'
    public static final String ADMIN_USER = 'ADMIN'

    public static final String VERSIONS_PATH = '/versions'
    public static final String NON_EXISTING_API_VERSION = 'v0'

    public static final String V1_PATH_STUDIES = "/v1/studies"
    public static final String V1_PATH_observations = "/v1/observations"
    public static final String V1_PATH_PATIENT_SETS = "/v1/patient_sets"

    public static final String PATH_OBSERVATIONS = "/v2/observations"
    public static final String PATH_AGGREGATE = "/v2/observations/aggregate"
    public static final String PATH_COUNTS = "/v2/observations/count"
    public static final String PATH_SUPPORTED_FIELDS = "/v2/supported_fields"
    public static final String PATH_PATIENTS = "/v2/patients"
    public static final String PATH_TREE_NODES = "/v2/tree_nodes"
    public static final String PATH_STUDIES = "/v2/studies"
    public static final String PATH_PATIENT_SET = "/v2/patient_sets"
    public static final String PATH_STORAGE = "/v2/storage"
    public static final String PATH_FILES = "/v2/files"
    public static final String PATH_ARVADOS_WORKFLOWS = "/v2/arvados/workflows"
    public static final String PATH_DIMENSION = "/v2/dimensions"
    public static final String PATH_DATA_EXPORT = "/v2/export"
    public static final String PATH_QUERY = "/v2/queries"

    //study ids
    public static final String ORACLE_1000_PATIENT_ID = 'ORACLE_1000_PATIENT'
    public static final String GSE8581_ID = 'GSE8581'
    public static final String CELL_LINE_ID = 'CELL-LINE'
    public static final String EHR_ID = 'EHR'
    public static final String EHR_HIGHDIM_ID = 'EHR_HIGHDIM'
    public static final String CLINICAL_TRIAL_ID = 'CLINICAL_TRIAL'
    public static final String CLINICAL_TRIAL_HIGHDIM_ID = 'CLINICAL_TRIAL_HIGHDIM'
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
    public static final boolean AUTH_NEEDED = true
    public static final boolean DEBUG = false
    public static final boolean SUPPRESS_KNOWN_BUGS = true
    public static final boolean SUPPRESS_UNIMPLEMENTED = true
    public static final boolean RUN_HUGE_TESTS = false

}
