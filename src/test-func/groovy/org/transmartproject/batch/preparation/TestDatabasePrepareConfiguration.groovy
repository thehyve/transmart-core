package org.transmartproject.batch.preparation

import groovy.transform.TypeChecked
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.mapping.DefaultLineMapper
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.JdbcTemplate
import org.transmartproject.batch.beans.AppConfig
import org.transmartproject.batch.beans.StepScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables

import javax.sql.DataSource

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

    @Autowired
    HeaderSavingLineCallbackHandler headerSavingLineCallbackHandler

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        new JdbcTemplate(dataSource)
    }

    @Bean
    Job fillTablesJob() {
        jobs.get('fillTablesJob')
                .start(fillTablesFlow())
                .end()
                .build()
    }

    @Bean
    Flow fillTablesFlow() {
        new FlowBuilder<SimpleFlow>('fillTablesFlow')
                .start(loadModifierDimension())
                .next(loadModifierMetadata())
                .build()
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
    @StepScopeInterfaced
    ItemWriter<FieldSet> modifierPathWriter() {
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
    @StepScopeInterfaced
    ItemWriter<FieldSet> modifierMetadataWriter() {
        fieldSetJdbcWriter(Tables.MODIFIER_METADATA)
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
                skippedLinesCallback: headerSavingLineCallbackHandler,

                resource: resource,
                strict: true,
        )
    }

    // has to be bean!
    private ItemWriter<FieldSet> fieldSetJdbcWriter(String table) {
        new TsvFieldSetJdbcBatchItemWriter(table: table)
    }

}
