package example

/**
 *
 */
class RunJob {

    static void main(String ... args) {
        def jobPath = 'example.ExampleConfiguration'
        def jobId = 'job1'
        org.springframework.batch.core.launch.support.CommandLineJobRunner.main(jobPath, jobId)
    }
}
