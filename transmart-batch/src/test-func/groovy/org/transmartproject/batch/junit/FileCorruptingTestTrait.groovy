package org.transmartproject.batch.junit

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.batch.item.file.mapping.DefaultLineMapper
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy
import org.springframework.batch.item.file.transform.DelimitedLineAggregator
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.FieldExtractor
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.transmartproject.batch.startup.RunJob

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.springframework.batch.item.file.transform.DelimitedLineTokenizer.DELIMITER_TAB

/**
 * Mixin for tests to make it easier to corrupt and fix files in order to
 * exercise job resuming code paths.
 */
@SuppressWarnings('BracesForClassRule') // buggy with traits
trait FileCorruptingTestTrait {

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Autowired
    JobLauncher jobLauncher

    @Autowired
    JobRepository repository

    private JobParameters lastRunParameters
    private JobExecution lastExecution

    File corruptFile(File originalFile, int line, int column, String newValue) {
        // copy data and corrupt it
        alterFile(originalFile) {
            it[line][column] = newValue
            null
        }
    }

    File alterFile(File originalFile,
                   @ClosureParams(value = FromString, options = ['List<List<String>>']) Closure<Void> closure) {
        // copy data and modify it
        File corruptedFile = temporaryFolder.newFile()

        List<List<String>> fileContent = parseFile(originalFile)
        closure.call(fileContent)
        writeFile(corruptedFile, fileContent)

        corruptedFile
    }

    List<List<String>> parseFile(File file) {
        def reader = new FlatFileItemReader<FieldSet>(
                lineMapper: new DefaultLineMapper<FieldSet>(
                        lineTokenizer: new DelimitedLineTokenizer(DELIMITER_TAB),
                        fieldSetMapper: new PassThroughFieldSetMapper(),
                ),
                recordSeparatorPolicy: new DefaultRecordSeparatorPolicy(),
                resource: new FileSystemResource(file),
                saveState: false)

        def result = []
        FieldSet fieldSet
        reader.open(new ExecutionContext())
        while ((fieldSet = reader.read()) != null) {
            result << (fieldSet.values as List)
        }
        reader.close()

        result
    }

    void writeFile(File destination, List<List<String>> contents) {
        def writer = new FlatFileItemWriter<List<String>>(
                lineAggregator: new DelimitedLineAggregator(
                        delimiter: DELIMITER_TAB,
                        fieldExtractor: { it as Object[] } as FieldExtractor),
                resource: new FileSystemResource(destination),
                saveState: false,
                transactional: false,)

        writer.open(new ExecutionContext())
        writer.write contents
        writer.close()
    }

    void firstExecution(List<String> params) {
        def runJob = RunJob.createInstance(*params)
        def intResult = runJob.run()
        assertThat 'first execution is unsuccessful', intResult, is(1)

        lastRunParameters = runJob.finalJobParameters
        lastExecution = repository.getLastJobExecution(
                runJob.jobName, lastRunParameters)
    }

    void secondExecution(List<String> params) {
        def runJob = RunJob.createInstance(*params,
                '-r', '-j', lastExecution.id as String)
        def intResult = runJob.run()
        assertThat 'second execution is successful', intResult, is(0)
    }
}
