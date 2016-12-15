package org.transmartproject.batch.ontologymapping

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.mapping.FieldSetMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.transmartproject.batch.batchartifacts.PutInBeanWriter
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.ClinicalJobContext
import org.transmartproject.batch.clinical.ClinicalJobSpecification
import org.transmartproject.batch.clinical.ontology.OntologyNode
import org.transmartproject.batch.clinical.variable.ClinicalVariable
import org.transmartproject.batch.clinical.variable.ColumnMappingFileHeaderHandler
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Spring configuration for the ontology mapping fetching job.
 */
@Configuration
@ComponentScan(['org.transmartproject.batch.clinical',
                'org.transmartproject.batch.ontologymapping'])
class FetchOntologyMappingJobConfiguration extends AbstractJobConfiguration {

    static final String JOB_NAME = 'FetchOntologyMappingJob'
    static final int CHUNK_SIZE = 100

    @Bean(name = 'FetchOntologyMappingJob')
    Job job() {
        jobs.get(JOB_NAME)
                .start(readVariablesStep())
                .next(ontologyMappingExportStep())
                .build()
    }

    @Bean
    Step readVariablesStep() {
        TaskletStep s = steps.get('readVariables')
                .allowStartIfComplete(true)
                .chunk(5)
                .reader(clinicalVariablesReader(null, null))
                .writer(saveClinicalVariableList(null))
                .build()
        s
    }

    @Bean
    ItemStreamReader<ClinicalVariable> clinicalVariablesReader(
            FieldSetMapper<ClinicalVariable> clinicalVariableFieldMapper,
            ColumnMappingFileHeaderHandler columnMappingFileHeaderValidatingHandler) {
        tsvFileReader(
                columnMapFileResource(),
                mapper: clinicalVariableFieldMapper,
                saveHeader: columnMappingFileHeaderValidatingHandler,
                allowMissingTrailingColumns: true,
                linesToSkip: 1,)
    }

    @Bean
    @JobScope
    PutInBeanWriter<ClinicalVariable> saveClinicalVariableList(ClinicalJobContext ctx) {
        new PutInBeanWriter<ClinicalVariable>(bean: ctx.variables)
    }

    @Bean
    @JobScopeInterfaced
    Resource columnMapFileResource() {
        new JobParameterFileResource(parameter: ClinicalJobSpecification.COLUMN_MAP_FILE)
    }

    @Bean
    Step ontologyMappingExportStep() {
        TaskletStep s = steps.get('tagTypesExportStep')
                .chunk(CHUNK_SIZE)
                .reader(ontologyMappingGenerator())
                .writer(ontologyMappingWriter())
                .faultTolerant()
                .processorNonTransactional()
                .retryLimit(0) // do not retry individual items
                .listener(logCountsStepListener())
                .listener(lineOfErrorDetectionListener())
                .build()
        s
    }

    @Bean
    @JobScope
    OntologyMappingGenerator ontologyMappingGenerator() {
        new OntologyMappingGenerator()
    }

    @Bean
    @JobScope
    OntologyMappingWriter ontologyMappingWriter() {
        Resource resource = ontologyMappingFileResource(null)
        new OntologyMappingWriter(resource)
    }

    @Bean
    @JobScope
    FileSystemResource ontologyMappingFileResource(@Value("#{jobParameters}") Map<String, Object> jobParameters) {
        new FileSystemResource(jobParameters[FetchOntologyMappingJobSpecification.ONTOLOGY_MAP_FILE])
    }

}
