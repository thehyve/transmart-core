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

    public static final String DEFAULT_JOB_NAME = 'job'

    private Map<String, Class<? extends ExternalJobParameters>> parametersTypeMap = [
            'clinical': ClinicalExternalJobParameters,
    ]

    OptionAccessor opts

    private final static String USAGE = '''
transmart-batch-capsule.jar -p <params file>
                            [ -d <param=value> | [ -d <param2=value2> | ... ]]
                            [ -c <file> ] [-j <job name> ]
                            [ -r | -s | -a | -n ]'''

    private static CliBuilder createCliBuilder() {
        def cli = new CliBuilder(usage: USAGE)
        cli.writer = new PrintWriter(System.err)
        cli.d args: 2, valueSeparator: '=', argName: 'param=value', 'override/supplement params file parameter'
        cli.p args: 1, argName: 'file location', 'specify params file', required: true
        cli.c longOpt: 'config', args: 1, 'location of database configuration properties file ' +
                '(default: ~/.transmart/batchdb.properties)'
        cli.j longOpt: 'jobName', args: 1, "an arbitrary name for the job (default: $DEFAULT_JOB_NAME)"
        cli.r longOpt: 'restart', 'restart the last failed execution'
        cli.s longOpt: 'stop', 'stop a running execution'
        cli.a longOpt: 'abandon', 'abandon a stopped execution'
        cli.n longOpt: 'next', 'start the next in a sequence according to the JobParametersIncrementer'
        cli.h longOpt: 'help', 'show this help message'
        cli
    }

    static void main(String ... args) {
        CommandLineJobRunner.presetSystemExiter(new OnErrorSystemExiter())

        def cliBuilder = createCliBuilder()
        OptionAccessor opts = cliBuilder.parse(args)
        if (!opts) {
            cliBuilder.usage()
            System.exit 1
        }

        def runJob = new RunJob(opts: opts)
        runJob.run()
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
        runner.jobParametersConverter = new JobParametersConverter() {
            JobParameters getJobParameters(Properties properties) {
                externalJobParams as JobParameters
            }
            Properties getProperties(JobParameters params) {
                externalJobParams as Properties
            }
        }
        runner.start(externalJobParameters.jobPath.name,
                jobName,
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

    String getJobName() {
        opts.j ?: DEFAULT_JOB_NAME
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
