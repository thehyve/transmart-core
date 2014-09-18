package example

import org.transmartproject.batch.clinical.ClinicalDataLoadJobConfiguration
import org.transmartproject.batch.support.Keys

/**
 *
 */
class RunJob {

    static void main(String ... args) {
        transmart()
    }

    static void transmart() {
        def jobPath = ClinicalDataLoadJobConfiguration.class.name
        def jobId = 'job'
        execute(jobPath, jobId, gse8581())
    }

    static void simpleExample() {
        def jobPath = 'example.ExampleConfiguration'
        def jobId = 'job1'
        execute(jobPath, jobId)
    }

    static void execute(String jobPath, String jobId, Map<String,String> args) {
        List<String> list = [jobPath, jobId]
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
