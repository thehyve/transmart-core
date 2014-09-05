package example

import org.transmartproject.batch.Keys

/**
 *
 */
class RunJob {

    static void main(String ... args) {
        transmart()
    }

    static void transmart() {
        def jobPath = 'org.transmartproject.batch.JobConfiguration'
        def jobId = 'job'
        def args = [
                (Keys.COLUMN_MAP_FILE): 'src/test/resources/clinical/E-GEOD-8581_columns.txt',
                (Keys.WORD_MAP_FILE): 'src/test/resources/clinical/E-GEOD-8581_wordmap.txt'
        ]
        execute(jobPath, jobId, args)
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

}
