package org.transmartproject.batch.clinical

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.FlowJob
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.UpdateQueryBuilder
import org.transmartproject.batch.model.DemographicVariable
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.tasklet.DeleteConceptCountsTasklet
import org.transmartproject.batch.tasklet.InsertConceptCountsTasklet

@Configuration
class ClinicalDataLoadJobConfiguration extends AbstractJobConfiguration {

    public static final String JOB_NAME = 'ClinicalDataLoadJob'

    @Bean
    Job job() {
        FlowJob job =
            jobs.get(JOB_NAME)
                    .start(mainFlow())
                    .end()
                    .build()
        job.jobParametersIncrementer = jobParametersIncrementer()
        job
    }

    @Bean
    Flow mainFlow() {
        new FlowBuilder<SimpleFlow>('mainFlow')
                .start(readControlFilesFlow())                              //reads control files (column map, word map)
                .next(stepOf(this.&gatherCurrentPatientsTasklet))           //get patients from database
                .next(stepOf(this.&gatherCurrentConceptsTasklet))           //get concepts from database
                .next(stepOf(this.&deleteObservationFactTasklet))
                .next(stepOf(this.&deleteConceptCountsTasklet))
                .next(rowProcessingStep())                                  //process rows, inserting observation_fact rows
                .next(stepOf(this.&insertUpdatePatientDimensionTasklet))    //insert/update patient_dimension
                .next(stepOf(this.&insertPatientTrialTasklet))              //insert patient_trial rows
                .next(stepOf(this.&insertConceptsTasklet))                  //insert i2b2, i2b2_secure and concept_dimension rows
                .next(stepOf(this.&insertConceptCountsTasklet))             //insert concept_counts rows
                .build()
    }

    @Bean
    Flow readControlFilesFlow() {
        parallelFlowOf(
                'readControlFilesFlow',
                stepOf(this.&readVariablesTasklet),
                stepOf(this.&readWordMapTasklet),
        )
    }

    @Bean
    Step rowProcessingStep() {
        steps.get('rowProcessingStep')
                .chunk(5)
                .reader(dataRowReader()) //read data
                .processor(compositeOf(
                    wordReplaceProcessor(), //replace words, if such is configured
                    rowToFactRowSetConverter(), //converts each Row to a FactRowSet
                ))
                .writer(factRowSetTableWriter()) //writes the FactRowSets in lt_src_clinical_data
                .build()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet readWordMapTasklet() {
        new ReadWordMapTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet readVariablesTasklet() {
        new ReadVariablesTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet gatherCurrentPatientsTasklet() {
        new GatherCurrentPatientsTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet gatherCurrentConceptsTasklet() {
        new GatherCurrentConceptsTasklet()
    }

    @Bean
    @StepScope
    DataRowReader dataRowReader() {
        new DataRowReader()
    }

    @Bean
    @StepScopeInterfaced
    ItemProcessor<Row,Row> wordReplaceProcessor() {
        new WordReplaceItemProcessor()
    }

    @Bean
    @StepScopeInterfaced
    ItemProcessor<Row, FactRowSet> rowToFactRowSetConverter() {
        new RowToFactRowSetConverter()
    }

    @Bean
    ItemWriter<FactRowSet> factRowSetTableWriter() {
        new ObservationFactTableWriter()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet insertUpdatePatientDimensionTasklet() {
        new InsertUpdatePatientDimensionTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet insertPatientTrialTasklet() {
        new InsertPatientTrialTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet insertConceptsTasklet() {
        new InsertConceptsTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet insertConceptCountsTasklet() {
        new InsertConceptCountsTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet deleteObservationFactTasklet() {
        new DeleteObservationFactTasklet()
    }

    @Bean
    @JobScopeInterfaced
    Tasklet deleteConceptCountsTasklet() {
        new DeleteConceptCountsTasklet()
    }
}
