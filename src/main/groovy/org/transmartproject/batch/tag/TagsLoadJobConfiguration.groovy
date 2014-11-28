package org.transmartproject.batch.tag

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.SimpleJob
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.ClinicalExternalJobParameters
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Spring configuration for the tag load job.
 */
@Configuration
@ComponentScan('org.transmartproject.batch.tag')
class TagsLoadJobConfiguration extends AbstractJobConfiguration {

    static final String JOB_NAME = 'TagsLoadJob'
    static final int CHUNK_SIZE = 100

    @Bean(name = 'TagsLoadJob')
    @Override
    Job job() {
        SimpleJob job =
                jobs.get(JOB_NAME)
                        .start(tagsLoadStep())
                        .build()
        job.jobParametersIncrementer = jobParametersIncrementer
        job
    }

    @Bean
    Step tagsLoadStep() {
        steps.get('tagsLoadStep')
                .chunk(CHUNK_SIZE)
                .reader(tagReader())
                .writer(tagTsvWriter())
                .build()
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
    ItemWriter tagTsvWriter() {
        new TagWriter()
    }

    @Bean
    @JobScopeInterfaced
    Resource tagsFileResource() {
        new JobParameterFileResource(parameter: ClinicalExternalJobParameters.TAGS_FILE)
    }

}
