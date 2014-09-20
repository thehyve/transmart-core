package org.transmartproject.batch

import org.springframework.batch.core.launch.support.CommandLineJobRunner
import org.springframework.batch.core.launch.support.SystemExiter
import org.transmartproject.batch.clinical.ClinicalDataLoadJobConfiguration
import org.transmartproject.batch.support.Keys

/**
 *
 */
class RunJob {

    static void main(String ... args) {
        CommandLineJobRunner.presetSystemExiter(new DummySystemExiter())
        //transmart(gse8581())
        transmart(gse20690()) //this example has multiple rows for the same patient (with different visit names)
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
        args.put('date', new Date().toString())
        List<String> list = [jobPath, jobId]
        System.setProperty('propertySource', getBatchPropertiesPath())
        args.each { list.add("$it.key=$it.value") }
        list.add(0, '-next') //trigger incrementing

        CommandLineJobRunner.main(list as String[])
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
                (Keys.COLUMN_MAP_FILE): 'Rheumatoid Arthritis_Takeuchi_GSE20690_Mapping_File.txt',
        ]
    }

    static class DummySystemExiter implements SystemExiter {
        @Override
        void exit(int status) {
            if (status != 0) {
                System.exit(status)
            }
        }
    }

}
