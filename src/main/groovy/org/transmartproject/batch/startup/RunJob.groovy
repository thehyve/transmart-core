package org.transmartproject.batch.startup

import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.converter.JobParametersConverter
import org.springframework.batch.core.launch.support.CommandLineJobRunner

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Entry point for the application.
 */
@SuppressWarnings(['SystemExit', 'SystemErrPrint'])
final class RunJob {

    public static final String DEFAULT_BATCHDB_PROPERTIES_LOCATION = 'file:./batchdb.properties'

    OptionAccessor opts

    JobParameters finalJobParameters

    String jobName

    private final static String USAGE = '''transmart-batch-capsule.jar -p <params file>
    [ -d <param=value> | [ -d <param2=value2> | ... ]]
    [ -c <file> ]
    (
        ((-r | -s | -a ) -j <job id>)) |
        [-n])'''

    private static CliBuilder createCliBuilder() {
        def cli = new CliBuilder(usage: USAGE)
        cli.writer = new PrintWriter(System.err)
        cli.with {
            d args: 2, valueSeparator: '=', argName: 'param=value', 'override/supplement params file parameter'
            p args: 1, argName: 'file location', 'specify params file', required: true
            c longOpt: 'config', args: 1, 'location of database configuration properties file ' +
                    "(default: $DEFAULT_BATCHDB_PROPERTIES_LOCATION)"
            j longOpt: 'jobIdentifier', args: 1, "the id or name of a job instance"
            r longOpt: 'restart', 'restart the last failed execution'
            s longOpt: 'stop', 'stop a running execution'
            a longOpt: 'abandon', 'abandon a stopped execution'
            n longOpt: 'next', 'start the next in a sequence according to the JobParametersIncrementer'
            it
        }
    }

    static void main(String... args) {
        def runJob = createInstance(args)

        def exitCode = runJob.run()

        if (exitCode != 0) {
            System.exit exitCode
        }
    }

    static RunJob createInstance(String... args) {
        def cliBuilder = createCliBuilder()
        OptionAccessor opts = cliBuilder.parse(args)
        if (!opts) {
            System.exit 1
        }
        new RunJob(opts: opts)
    }

    int run() {
        def propSource = batchPropertiesPath
        if (!propSource) {
            System.exit 1
        }
        System.setProperty 'propertySource', propSource

        def jobInitializationData = this.jobStartupDetails

        def runner = new CommandLineJobRunner()
        JobParametersConverter jobParametersConverter = getJobParametersConverterForJobDetails(jobInitializationData)
        finalJobParameters = jobParametersConverter.getJobParameters(null)
        runner.jobParametersConverter = jobParametersConverter

        String jobIdentifier = calculateJobIdentifier(jobInitializationData.jobPath)
        if (!jobIdentifier) {
            System.exit 1
        }
        jobName = jobInitializationData.jobPath.JOB_NAME

        runner.start(jobInitializationData.jobPath.name,
                jobIdentifier,
                [] as String[] /* converter above takes care of params */,
                (
                        [] +
                                (opts.r ? '-restart' : []) +
                                (opts.s ? '-stop' : []) +
                                (opts.a ? '-abandon' : []) +
                                (opts.n ? '-next' : [])) as Set)
    }

    private static JobParametersConverter getJobParametersConverterForJobDetails(final JobStartupDetails jobDetails) {
        new JobParametersConverter() {
            @SuppressWarnings('UnusedMethodParameter')
            JobParameters getJobParameters(Properties properties) {
                jobDetails as JobParameters
            }

            @SuppressWarnings('UnusedMethodParameter')
            Properties getProperties(JobParameters params) {
                jobDetails as Properties
            }
        }
    }

    String getBatchPropertiesPath() {
        if (!opts.c) {
            DEFAULT_BATCHDB_PROPERTIES_LOCATION
        } else {
            Path path = Paths.get((String) opts.c)
            if (!Files.isReadable(path) || !Files.isRegularFile(path)) {
                throw new InvalidParametersFileException("'$path' is not a readable file")

            }
            "file:${path.toAbsolutePath()}"
        }
    }

    Path getParamsFilePath() {
        Path path = Paths.get((String) opts.p)
        if (!Files.isReadable(path) || !Files.isRegularFile(path)) {
            throw new InvalidParametersFileException("'$path' is not a readable file")
        }
        path
    }

    String calculateJobIdentifier(Class configurationClass) {
        /* CommandLineJobRunner uses -j both for a bean job name
         * (or logical job name to be found by a JobLocator) or
         * a JobInstance identifier or name.
         *
         * The job name should also match its bean name because
         * on restarts the job name saved in the registry is
         * used as the bean name
         */
        if (opts.r || opts.s || opts.a) {
            if (!opts.j) {
                System.err.println('The -j parameter must be specified ' +
                        'when the options -n, -s or -a are used')
                null
            } else {
                opts.j
            }
        } else {
            def job = configurationClass.JOB_NAME
            if (!job) {
                throw new IllegalStateException("Class $configurationClass should " +
                        "have a static property 'JOB_NAME'")
            }

            job
        }
    }

    JobStartupDetails getJobStartupDetails() {
        def ds = opts.ds
        def paramOverrides = [:]
        if (ds) {
            for (int i = 0; i < ds.size(); i += 2) {
                paramOverrides[ds[i]] = ds[i + 1]
            }
        }

        def paramsFile = paramsFilePath

        try {
            JobStartupDetails.fromFile(paramsFile, paramOverrides)
        } catch (InvalidParametersFileException e) {
            throw new InvalidParametersFileException(e)
        }
    }
}
