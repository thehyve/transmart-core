package org.transmartproject.batch.tag

import com.google.common.collect.Lists
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.file.FlatFileItemReader
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
import org.transmartproject.batch.clinical.ClinicalJobSpecification
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.support.JobParameterFileResource

import static org.transmartproject.batch.batchartifacts.DbMetadataBasedBoundsValidator.c

/**
 * Spring configuration for the tag load job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.tag',
                'org.transmartproject.batch.concept'])
class TagsLoadJobConfiguration extends AbstractJobConfiguration {

    static final String JOB_NAME = 'TagsLoadJob'
    static final int CHUNK_SIZE = 100

    @javax.annotation.Resource
    Tasklet validateTopNodePreexistenceTasklet

    @javax.annotation.Resource
    Tasklet gatherCurrentConceptsTasklet

    @Bean(name = 'TagsLoadJob')
    @Override
    Job job() {
        jobs.get(JOB_NAME)
                .start(stepOf(this.&getGatherCurrentConceptsTasklet))
                .next(stepOf(this.&getValidateTopNodePreexistenceTasklet))
                .next(tagsLoadStep(null, null))
                .build()
    }

    @Bean
    Step tagsLoadStep(TagValidator tagValidator,
                      TagConceptExistenceValidator tagConceptExistenceValidator) {
        TaskletStep s = steps.get('tagsLoadStep')
                .chunk(CHUNK_SIZE)
                .reader(tagReader())
                .processor(compositeOf(
                        new ValidatingItemProcessor(adaptValidator(tagValidator)),
                        duplicationDetectionProcessor(),
                        new ValidatingItemProcessor(tagConceptExistenceValidator),
                ))
                .writer(tagTsvWriter())
                .faultTolerant()
                .processorNonTransactional()
                .retryLimit(0) // do not retry individual items
                .skip(NoSuchConceptException)
                .noRollback(NoSuchConceptException)
                .skipLimit(Integer.MAX_VALUE)
                .listener(logCountsStepListener())
                .listener(lineOfErrorDetectionListener())
                .build()

        s.streams = duplicationDetectionProcessor()
        s
    }

    @Bean
    @JobScope
    FlatFileItemReader<Tag> tagReader() {
        tsvFileReader(
                tagsFileResource(),
                beanClass: Tag,
                columnNames: ['concept_fragment', 'tag_title', 'tag_description', 'index'],
                linesToSkip: 1)
    }

    @Bean
    @JobScopeInterfaced
    Validator tagBoundsValidator() {
        new DbMetadataBasedBoundsValidator(
                Tag,
                tagTitle: c(Tables.I2B2_TAGS, 'tag_type'),
                tagDescription: c(Tables.I2B2_TAGS, 'tag'))
    }

    @Bean
    @StepScope
    DuplicationDetectionProcessor<Tag> duplicationDetectionProcessor() {
        new DuplicationDetectionProcessor<Tag>(
                calculateKey: { Tag t ->
                    Lists.asList(t.conceptFragment, t.tagTitle)
                })
    }

    @Bean
    @JobScope
    TagWriter tagTsvWriter() {
        new TagWriter()
    }

    @Bean
    @JobScopeInterfaced
    Resource tagsFileResource() {
        new JobParameterFileResource(
                parameter: ClinicalJobSpecification.TAGS_FILE)
    }

}
