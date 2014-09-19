package org.transmartproject.batch

import org.transmartproject.batch.clinical.ClinicalDataLoadJobConfiguration
import org.transmartproject.batch.support.Keys

/**
 *
 */
class RunJob {

    static void main(String ... args) {
        transmart(gse8581())
        transmart(gse20690()) //@TODO 2nd job run reuses the beans (and job parameters) of the 1st
    }

    static void transmart(Map study) {
        def jobPath = ClinicalDataLoadJobConfiguration.class.name
        def jobId = 'job'
        execute(jobPath, jobId, study)
    }

    static String getBatchPropertiesPath() {
        String userHome = System.getProperty('user.home')
        "file:$userHome/.transmart/batchdb.properties"
    }

    static void execute(String jobPath, String jobId, Map<String,String> args) {
        List<String> list = [jobPath, jobId]
        System.setProperty('propertySource', getBatchPropertiesPath())
        args.each { list.add("$it.key=$it.value") }
        org.springframework.batch.core.launch.support.CommandLineJobRunner.main(list as String[])
    }

    static Map gse8581() {
        [
                (Keys.STUDY_ID): 'GSE8581',
                (Keys.DATA_LOCATION): 'src/test/resources/clinical/GSE8581',
                (Keys.COLUMN_MAP_FILE): 'E-GEOD-8581_columns.txt',
                (Keys.WORD_MAP_FILE): 'E-GEOD-8581_wordmap.txt'
        ]
    }

    static Map gse20690() {
        [
                (Keys.STUDY_ID): 'GSE20690',
                (Keys.DATA_LOCATION): 'src/test/resources/clinical/GSE20690',
                (Keys.COLUMN_MAP_FILE): 'E-GEOD-8581_columns.txt',
        ]
    }

}
