package org.transmartproject.batch.highdim.platform.chrregion

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.highdim.platform.PlatformLoadJobConfiguration

import javax.annotation.Resource

/**
 * Spring configuration for the chromosomal region data job.
 */
@Configuration
@ComponentScan('org.transmartproject.batch.highdim.platform.chrregion')
class ChromosomalRegionJobConfiguration extends PlatformLoadJobConfiguration {

    public static final String JOB_NAME = 'ChromosomalRegionLoadJob'

    static int chunkSize = 5000

    @Resource
    Tasklet deleteChromosomalRegionTasklet

    @Autowired
    ChromosomalRegionRowValidator chromosomalRegionRowValidator

    @Autowired
    ChromosomalRegionRowWriter chromosomalRegionRowWriter

    @Bean(name = 'ChromosomalRegionLoadJob' /* JOB_NAME */)
    @Override
    Job job() {
        jobs.get(JOB_NAME)
                .start(mainFlow(null))
                .end()
                .build()
    }

    @Bean
    Step mainStep() {
        steps.get('mainStep')
                .chunk(chunkSize)
                .reader(chromosomalRegionRowReader(null))
                .processor(new ValidatingItemProcessor(adaptValidator(chromosomalRegionRowValidator)))
                .writer(chromosomalRegionRowWriter)
                .listener(lineOfErrorDetectionListener())
                .listener(progressWriteListener())
                .build()
    }

    @Bean
    @JobScope //Why job scope?
    FlatFileItemReader<ChromosomalRegionRow> chromosomalRegionRowReader(
            org.springframework.core.io.Resource annotationsFileResource) {
        tsvFileReader(
                annotationsFileResource,
                linesToSkip: 1,
                beanClass: ChromosomalRegionRow,
                columnNames: ['gplId', 'regionName', 'chromosome', 'startBp', 'endBp', 'numProbes', 'cytoband',
                              'geneSymbol', 'geneId', 'organism'])
    }

    @Bean
    Step deleteAnnotationsStep() {
        stepOf('deleteChromosomalRegionTasklet', deleteChromosomalRegionTasklet)
    }

}
