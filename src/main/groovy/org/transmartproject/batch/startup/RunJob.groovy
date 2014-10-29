package org.transmartproject.batch.startup

import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.converter.JobParametersConverter
import org.springframework.batch.core.launch.support.CommandLineJobRunner
import org.springframework.batch.core.launch.support.SystemExiter
import org.transmartproject.batch.clinical.ClinicalExternalJobParameters

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

final class RunJob {

    public static final String DEFAULT_JOB_BEAN_NAME = 'job'

    private Map<String, Class<? extends ExternalJobParameters>> parametersTypeMap = [
            'clinical': ClinicalExternalJobParameters,
    ]

    OptionAccessor opts

    JobParameters finalJobParameters

    private final static String USAGE = '''
transmart-batch-capsule.jar -p <params file>
                            [ -d <param=value> | [ -d <param2=value2> | ... ]]
                            [ -c <file> ]
                            (
                              ((-r | -s | -a ) -j <job identifier>)) |
                              [-n])'''

    private static CliBuilder createCliBuilder() {
        def cli = new CliBuilder(usage: USAGE)
        cli.writer = new PrintWriter(System.err)
        cli.d args: 2, valueSeparator: '=', argName: 'param=value', 'override/supplement params file parameter'
        cli.p args: 1, argName: 'file location', 'specify params file', required: true
        cli.c longOpt: 'config', args: 1, 'location of database configuration properties file ' +
                '(default: ~/.transmart/batchdb.properties)'
        cli.j longOpt: 'jobIdentifier', args: 1, "the id or name of a job instance"
        cli.r longOpt: 'restart', 'restart the last failed execution'
        cli.s longOpt: 'stop', 'stop a running execution'
        cli.a longOpt: 'abandon', 'abandon a stopped execution'
        cli.n longOpt: 'next', 'start the next in a sequence according to the JobParametersIncrementer'
        cli.h longOpt: 'help', 'show this help message'
        cli
    }

    static void main(String... args) {
        CommandLineJobRunner.presetSystemExiter(new OnErrorSystemExiter())

        def cliBuilder = createCliBuilder()
        OptionAccessor opts = cliBuilder.parse(args)
        if (!opts) {
            cliBuilder.usage()
            System.exit 1
        }

        def runJob = createInstance(args)
        runJob.run()
    }

    static RunJob createInstance(String... args) {
        def cliBuilder = createCliBuilder()
        OptionAccessor opts = cliBuilder.parse(args)
        if (!opts) {
            cliBuilder.usage()
            System.exit 1
        }
        new RunJob(opts: opts)
    }

    void run() {
        def propSource = batchPropertiesPath
        if (!propSource) {
            System.exit 1
        }
        System.setProperty 'propertySource', propSource

        def externalJobParams = externalJobParameters
        if (!externalJobParams) {
            System.exit 1
        }

        def runner = new CommandLineJobRunner()
        finalJobParameters = externalJobParams as JobParameters
        runner.jobParametersConverter = new JobParametersConverter() {
            JobParameters getJobParameters(Properties properties) {
                finalJobParameters
            }
            Properties getProperties(JobParameters params) {
                externalJobParams as Properties
            }
        }

        String jobIdentifier = calculateJobIdentifier()
        if (!jobIdentifier) {
            System.exit 1
        }
        runner.start(externalJobParameters.jobPath.name,
                jobIdentifier,
                [] as String[] /* converter above takes care of params */,
                (
                        [] +
                                (opts.r ? '-restart' : []) +
                                (opts.s ? '-stop' : []) +
                                (opts.a ? '-abandon' : []) +
                                (opts.n ? '-next' : [])) as Set);
    }

    String getBatchPropertiesPath() {
        if (!opts.c) {
            String userHome = System.getProperty('user.home')
            "file:$userHome/.transmart/batchdb.properties"
        } else {
            Path path = Paths.get((String) opts.c)
            if (!Files.isReadable(path) || Files.isRegularFile(path)) {
                System.err.println "'$path' is not a readable file"
                return null
            }
            "file:${path.toAbsolutePath()}"
        }
    }

    Path getParamsFilePath() {
        Path path = Paths.get((String) opts.p)
        if (!Files.isReadable(path) || !Files.isRegularFile(path)) {
            System.err.println "'$path' is not a readable file"
            return null
        }
        path
    }

    String calculateJobIdentifier() {
        /* CommandLineJobRunner uses -j both for a bean job name
         * (or logical job name to be found by a JobLocator) or
         * a JobInstance identifier or name */
         if (opts.r || opts.s || opts.a) {
             if (!opts.j) {
                 System.err.println('The -j parameter must be specified ' +
                         'when the options -n, -s or -a are used')
             }
         } else {
             DEFAULT_JOB_BEAN_NAME
         }
    }

    ExternalJobParameters getExternalJobParameters() {
        def ds = opts.ds
        def paramOverrides = [:]
        if (ds) {
            for (int i = 0; i < ds.length / 2; i++) {
                paramOverrides[ds[i]] = ds[ds[i + 1]]
            }
        }

        def paramsFile = paramsFilePath
        if (!paramsFile) {
            return null
        }

        try {
            ExternalJobParameters.fromFile(parametersTypeMap, paramsFile, paramOverrides)
        } catch (InvalidParametersFileException e) {
            System.err.println "Invalid parameters file: ${e.message}"
            null
        }
    }

    static class OnErrorSystemExiter implements SystemExiter {
        @Override
        void exit(int status) {
            if (status != 0) {
                System.exit(status)
            }
        }
    }

}
