package org.transmartproject.batch.tag

import com.google.common.collect.Lists
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.validation.Validator
import org.transmartproject.batch.batchartifacts.DbMetadataBasedBoundsValidator
import org.transmartproject.batch.batchartifacts.DuplicationDetectionProcessor
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.support.JobParameterFileResource

import static org.transmartproject.batch.batchartifacts.DbMetadataBasedBoundsValidator.c

/**
 * Spring configuration for the tag load job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.tag',
                'org.transmartproject.batch.concept'])
class TagTypesLoadJobConfiguration extends AbstractJobConfiguration {

    static final String JOB_NAME = 'TagTypesLoadJob'
    static final int CHUNK_SIZE = 100

    @Bean(name = 'TagTypesLoadJob')
    @Override
    Job job() {
        jobs.get(JOB_NAME)
                .start(fetchTagTypesStep())
                .next(tagTypesLoadStep(null))
                .next(deleteTagTypesStep())
                .build()
    }

    @Bean
    @JobScope
    TagTypesJobMetadata tagTypesJobMetadata() {
        new TagTypesJobMetadata()
    }

    @Bean
    @JobScope
    FetchTagTypesTasklet fetchTagTypesTasklet() {
        new FetchTagTypesTasklet()
    }

    @Bean
    @JobScope
    Step fetchTagTypesStep() {
        allowStartStepOf('fetchTagTypesStep', fetchTagTypesTasklet())
    }

    @Bean
    @JobScope
    DeleteTagTypesTasklet deleteTagTypesTasklet() {
        new DeleteTagTypesTasklet()
    }

    @Bean
    @JobScope
    Step deleteTagTypesStep() {
        stepOf('deleteTagTypesStep', deleteTagTypesTasklet())
    }

    @Bean
    @JobScope
    Step tagTypesLoadStep(TagTypeValidator tagTypeValidator) {
        TaskletStep s = steps.get('tagTypesLoadStep')
                .allowStartIfComplete(true)
                .chunk(CHUNK_SIZE)
                .reader(tagTypeReader())
                .processor(compositeOf(
                    new ValidatingItemProcessor(adaptValidator(tagTypeValidator)),
                    duplicationTagTypeDetectionProcessor(),
                ))
                .writer(tagTypeWriter())
                .faultTolerant()
                .processorNonTransactional()
                .retryLimit(0) // do not retry individual items
                .skip(NoSuchConceptException)
                .noRollback(NoSuchConceptException)
                .skipLimit(Integer.MAX_VALUE)
                .listener(logCountsStepListener())
                .listener(lineOfErrorDetectionListener())
                .build()

        s.streams = duplicationTagTypeDetectionProcessor()
        s
    }

    @Bean
    @JobScopeInterfaced
    ItemStreamReader<TagType> tagTypeReader() {
        tsvFileReader(
                tagTypesFileResource(),
                columnNames: ['node_type', 'title', 'solr_field_name', 'value_type', 'shown_if_empty',
                              'values', 'index'],
                mapper: new TagTypeMapper(),
                linesToSkip: 1)
    }

    @Bean
    @JobScopeInterfaced
    Validator tagTypeBoundsValidator() {
        new DbMetadataBasedBoundsValidator(
                TagType,
                nodeType: c(Tables.I2B2_TAG_TYPES, 'node_type'),
                title: c(Tables.I2B2_TAG_TYPES, 'tag_type'),
                valueType: c(Tables.I2B2_TAG_TYPES, 'value_type'),
                solrFieldName: c(Tables.I2B2_TAG_TYPES, 'solr_field_name'),
                shownIfEmpty: c(Tables.I2B2_TAG_TYPES, 'shown_if_empty'),
                index: c(Tables.I2B2_TAG_TYPES, 'index'),
        )
    }

    @Bean
    @StepScope
    DuplicationDetectionProcessor<TagType> duplicationTagTypeDetectionProcessor() {
        new DuplicationDetectionProcessor<TagType>(
                calculateKey: { TagType t ->
                    Lists.asList(t.nodeType, t.title)
                })
    }

    @Bean
    @JobScopeInterfaced
    ItemWriter tagTypeWriter() {
        new TagTypeWriter()
    }

    @Bean
    @JobScopeInterfaced
    Resource tagTypesFileResource() {
        new JobParameterFileResource(
                parameter: TagTypesLoadJobSpecification.TAG_TYPES_FILE)
    }

}
