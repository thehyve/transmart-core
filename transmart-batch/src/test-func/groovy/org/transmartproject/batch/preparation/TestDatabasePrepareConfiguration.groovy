package org.transmartproject.batch.preparation

import groovy.transform.TypeChecked
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.CallableTaskletAdapter
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.mapping.DefaultLineMapper
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.AppConfig
import org.transmartproject.batch.batchartifacts.HeaderSavingLineCallbackHandler
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.db.OracleTableTruncator
import org.transmartproject.batch.db.PostgresTableTruncator
import org.transmartproject.batch.db.TableTruncator

import javax.sql.DataSource
import java.util.concurrent.Callable

/**
 * Context for job that prepares the database for functional tests.
 */
@Configuration
@Import(AppConfig)
@ComponentScan('org.transmartproject.batch.preparation')
@TypeChecked
class TestDatabasePrepareConfiguration {

    public static final int CHUNK_SIZE = 100

    @Autowired
    JobBuilderFactory jobs

    @Autowired
    StepBuilderFactory steps

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        new JdbcTemplate(dataSource)
    }

    @Bean
    Job testDatabasePrepareJob() {
        jobs.get('testDatabasePrepareJob')
                .start(flow(null, null))
                .end()
                .build()
    }

    @Bean
    Flow flow(ObservationFactPKFixupTasklet observationFactPKFixupTasklet,
              MappingTablesFixupTasklet mappingTablesFixupTasklet) {
        def fixupObservationFactPKStep = steps
                .get('fixupObservationFact')
                .tasklet(observationFactPKFixupTasklet)
                .build()

        def mappingTablesFixupStep = steps
                .get('mappingTablesFixup')
                .tasklet(mappingTablesFixupTasklet)
                .build()

        def truncateCodeLookupStep = steps
                .get('truncatCodeLookup')
                .tasklet(truncateCodeLookupTasklet(null))
                .build()

        new FlowBuilder<SimpleFlow>('fillTablesFlow')
                .start(fixupObservationFactPKStep)
                .next(mappingTablesFixupStep)
                .next(truncateCodeLookupStep)
                .next(loadModifierDimension())
                .next(loadModifierMetadata())
                .next(loadCodeLookup())
                .next(loadDeRcSnpInfo())
                .build()
    }

    @Bean
    DatabaseImplementationClassPicker databasePicker() {
        new DatabaseImplementationClassPicker()
    }

    @Bean
    NamedParameterJdbcTemplate namedParameterJdbcTemplate(
            JdbcTemplate jdbcTemplate) {
        new NamedParameterJdbcTemplate(jdbcTemplate)
    }

    @Bean
    TableTruncator tableTruncator(DatabaseImplementationClassPicker databasePicker) {
        databasePicker.instantiateCorrectClass(PostgresTableTruncator, OracleTableTruncator)
    }

    @Bean
    Tasklet truncateCodeLookupTasklet(TableTruncator tableTruncator) {
        new CallableTaskletAdapter(callable: { ->
            tableTruncator.truncate(Tables.CODE_LOOKUP, false)
            RepeatStatus.FINISHED
        } as Callable<RepeatStatus>)
    }

    @Bean
    Step loadModifierDimension() {
        steps.get('loadModifierDimension')
                .chunk(CHUNK_SIZE)
                .reader(tsvFileReader(new ClassPathResource('xtrial/modifier_dimension.tsv')))
                .writer(modifierPathWriter())
                .build()
    }

    @Bean
    @StepScope
    TsvFieldSetJdbcBatchItemWriter modifierPathWriter() {
        fieldSetJdbcWriter(Tables.MODIFIER_DIM)
    }


    @Bean
    Step loadModifierMetadata() {
        steps.get('loadModifierMetadata')
                .chunk(CHUNK_SIZE)
                .reader(tsvFileReader(new ClassPathResource('xtrial/modifier_metadata.tsv')))
                .writer(modifierMetadataWriter())
                .build()
    }

    @Bean
    @StepScope
    TsvFieldSetJdbcBatchItemWriter modifierMetadataWriter() {
        fieldSetJdbcWriter(Tables.MODIFIER_METADATA)
    }

    @Bean
    @StepScope
    TsvFieldSetJdbcBatchItemWriter codeLookupWriter() {
        fieldSetJdbcWriter(Tables.CODE_LOOKUP)
    }

    @Bean
    Step loadCodeLookup() {
        steps.get('loadCodeLookup')
                .chunk(CHUNK_SIZE)
                .reader(tsvFileReader(new ClassPathResource('i2b2/code_lookup.tsv')))
                .writer(codeLookupWriter())
                .build()
    }

    @Bean
    @StepScope
    TsvFieldSetJdbcBatchItemWriter loadDeRcSnpInfoWriter() {
        fieldSetJdbcWriter(Tables.RC_SNP_INFO)
    }

    @Bean
    Step loadDeRcSnpInfo() {
        steps.get('loadDeRcSnpInfo')
                .chunk(CHUNK_SIZE)
                .reader(tsvFileReader(new ClassPathResource('gwas/de_rc_snp_info.tsv')))
                .writer(loadDeRcSnpInfoWriter())
                .build()
    }

    @Bean
    @StepScope
    HeaderSavingLineCallbackHandler headerSavingLineCallbackHandler() {
        new HeaderSavingLineCallbackHandler()
    }

    private ItemStreamReader<FieldSet> tsvFileReader(Resource resource) {
        new FlatFileItemReader<FieldSet>(
                lineMapper: new DefaultLineMapper<FieldSet>(
                        lineTokenizer: new DelimitedLineTokenizer(
                                delimiter: DelimitedLineTokenizer.DELIMITER_TAB,
                        ),
                        fieldSetMapper: new PassThroughFieldSetMapper()
                ),

                linesToSkip: 1,
                skippedLinesCallback: headerSavingLineCallbackHandler(),

                resource: resource,
                strict: true,
        )
    }

    // has to be bean!
    private TsvFieldSetJdbcBatchItemWriter fieldSetJdbcWriter(String table) {
        new TsvFieldSetJdbcBatchItemWriter(table: table)
    }

}
