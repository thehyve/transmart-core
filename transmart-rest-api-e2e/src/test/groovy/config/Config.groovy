/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package config

import base.ContentTypeFor
import base.TestContext
import groovy.transform.CompileStatic
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.FromServer

import java.util.function.Function

import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.HttpVerb.GET

@CompileStatic
class Config {

    public static <T> T getProperty(String key, T defaultValue, Class<T> type) {
        T value
        String property = System.getProperty(key)
        if (property == null) {
            value = defaultValue
        } else {
            if (type == Boolean) {
                value = property.toBoolean() as T
            } else {
                value = property as T
            }
        }
        println "Configuration property '${key}': ${value} [${type.simpleName}]"
        value
    }

    // $ gradle -DbaseUrl=https://transmart-dev.thehyve.net/ test
    public static final String BASE_URL = getProperty('baseUrl', 'http://localhost:8070/', String)

     // Configure whether the currently used application for providing a REST API for a TranSMART supports the `v1` API.
     // In particular, if the application is transmart-api-server, this should be set to false.
    public static final Boolean IS_V1_API_SUPPORTED = getProperty('v1Supported', Boolean.FALSE, Boolean)

    static TestContext newTestContext() {
        new TestContext().setHttpBuilder(configure {
            request.uri = BASE_URL
            // custom parsers
            response.parser(ContentTypeFor.PROTOBUF) { ChainedHttpConfig cfg, FromServer fs ->
                ProtobufHelper.parse(fs.inputStream)
            }
            // custom interceptor
            execution.interceptor(GET) { ChainedHttpConfig cfg, Function fx ->
                // set default type for PATH_OBSERVATIONS
                if (cfg.request.uri.path == PATH_OBSERVATIONS && !cfg.request.uri.query.type) {
                    (cfg.request.uri.query as Map<String, String>).type = 'clinical'
                }
                fx.apply(cfg)
            }
        }).setAuthAdapter(new OauthAdapter())
    }

    public static final String DEFAULT_USER = 'test-public-user-1'
    public static final String UNRESTRICTED_USER = 'test-public-user-2'
    public static final String ADMIN_USER = 'admin'

    // Configure Keycloak settings
    public static final String AUTH_SERVER_URL = 'https://keycloak-dwh-test.thehyve.net/'
    public static final String REALM = 'transmart-dev'
    public static final String RESOURCE = 'transmart-client'
    public static final Map<String, String> USER_SUB_MAPPING = [
            (DEFAULT_USER)     : '09fe381f-7e6b-49ec-a600-a6c68e1210ee',
            (UNRESTRICTED_USER): 'a6fe7901-c53d-4dca-ad82-4addadee1111',
            (ADMIN_USER)       : 'd92ea2b2-ba88-4169-b392-3c2ac241f1a0'
    ]

    public static final String VERSIONS_PATH = '/versions'
    public static final String NON_EXISTING_API_VERSION = 'v0'

    public static final String V1_PATH_STUDIES = "/v1/studies"
    public static final String V1_PATH_OBSERVATIONS = "/v1/observations"
    public static final String V1_PATH_PATIENT_SETS = "/v1/patient_sets"

    public static final String PATH_CONFIG = "/v2/admin/system/config"
    public static final String AFTER_DATA_LOADING_UPDATE = "/v2/admin/system/after_data_loading_update"
    public static final String UPDATE_STATUS = "/v2/admin/system/update_status"

    public static final String PATH_OBSERVATIONS = "/v2/observations"
    public static final String PATH_TABLE = "/v2/observations/table"
    public static final String PATH_CROSSTABLE = "/v2/observations/crosstable"
    public static final String PATH_AGGREGATES_PER_CONCEPT = "/v2/observations/aggregates_per_concept"
    public static final String PATH_COUNTS = "/v2/observations/counts"
    public static final String PATH_COUNTS_PER_CONCEPT = "/v2/observations/counts_per_concept"
    public static final String PATH_COUNTS_PER_STUDY = "/v2/observations/counts_per_study"
    public static final String PATH_COUNTS_PER_STUDY_AND_CONCEPT = "/v2/observations/counts_per_study_and_concept"
    public static final String PATH_PATIENT_COUNTS_THRESHOLD = "/v2/patient_counts_threshold"
    public static final String PATH_SUPPORTED_FIELDS = "/v2/supported_fields"
    public static final String PATH_PATIENTS = "/v2/patients"
    public static final String PATH_CONCEPTS = "/v2/concepts"
    public static final String PATH_TREE_NODES = "/v2/tree_nodes"
    public static final String PATH_SYSTEM_CLEAR_CACHE = "/v2/admin/system/clear_cache"
    public static final String PATH_STUDIES = "/v2/studies"
    public static final String PATH_PATIENT_SET = "/v2/patient_sets"
    public static final String PATH_STORAGE = "/v2/storage"
    public static final String PATH_FILES = "/v2/files"
    public static final String PATH_ARVADOS_WORKFLOWS = "/v2/arvados/workflows"
    public static final String PATH_DIMENSION = "/v2/dimensions"
    public static final String PATH_DATA_EXPORT = "/v2/export"
    public static final String PATH_QUERY = "/v2/queries"
    public static final String PATH_RELATIONS = "/v2/pedigree/relations"
    public static final String PATH_RELATION_TYPES = "/v2/pedigree/relation_types"
    public static final String PATH_NOTIFICATIONS = "/v2/admin/notifications/notify"

    //study ids
    public static final String ORACLE_1000_PATIENT_ID = 'ORACLE_1000_PATIENT'
    public static final String GSE8581_ID = 'GSE8581'
    public static final String EHR_ID = 'EHR'
    public static final String EHR_HIGHDIM_ID = 'EHR_HIGHDIM'
    public static final String CLINICAL_TRIAL_ID = 'CLINICAL_TRIAL'
    public static final String CLINICAL_TRIAL_HIGHDIM_ID = 'CLINICAL_TRIAL_HIGHDIM'
    public static final String CATEGORICAL_VALUES_ID = 'CATEGORICAL_VALUES'
    public static final String TUMOR_NORMAL_SAMPLES_ID = 'TUMOR_NORMAL_SAMPLES'
    public static final String SHARED_CONCEPTS_A_ID = 'SHARED_CONCEPTS_STUDY_A'
    public static final String SHARED_CONCEPTS_B_ID = 'SHARED_CONCEPTS_STUDY_B'
    public static final String SHARED_CONCEPTS_RESTRICTED_ID = 'SHARED_CONCEPTS_STUDY_C_PRIV'
    public static final String RNASEQ_TRANSCRIPT_ID = 'RNASEQ_TRANSCRIPT'
    public static final String MIX_HD_ID = 'MIX_HD'
    public static final String SURVEY1_ID = 'SURVEY1'
    public static final String SURVEY2_ID = 'SURVEY2'
    public static final String CSR = 'CSR'

    //settings
    public static final boolean DB_MIGRATED = true
    public static final boolean AUTH_NEEDED = true
    public static final boolean DEBUG = false
    public static final boolean SUPPRESS_KNOWN_BUGS = true
    public static final boolean SUPPRESS_UNIMPLEMENTED = true
    public static final boolean RUN_HUGE_TESTS = false

}
