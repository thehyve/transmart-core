package org.transmartproject.batch.backout

import groovy.transform.TypeChecked
import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.support.CompositeItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.transmartproject.batch.batchartifacts.IterableItemReader
import org.transmartproject.batch.batchartifacts.LogCountsStepListener
import org.transmartproject.batch.batchartifacts.StringTemplateBasedDecider
import org.transmartproject.batch.beans.AbstractJobConfiguration
import org.transmartproject.batch.beans.StepScopeInterfaced
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.DeleteConceptCountsTasklet
import org.transmartproject.batch.concept.InsertConceptCountsTasklet
import org.transmartproject.batch.concept.oracle.OracleInsertConceptCountsTasklet
import org.transmartproject.batch.concept.postgresql.PostgresInsertConceptCountsTasklet
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.support.ExpressionResolver

import static org.transmartproject.batch.backout.NextBackoutModuleJobExecutionDecider.statusForModule

/**
 * Main configuration for backout job.
 */
@Configuration
@ComponentScan(value = 'org.transmartproject.batch',
        includeFilters = [
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        value = BackoutModule),
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        value = BackoutComponent)
        ],
        useDefaultFilters = false
)
@TypeChecked
class BackoutJobConfiguration extends BackoutJobConfigurationParent {

    public static final String JOB_NAME = 'BackoutJob'
    public static final int DELETE_CONCEPTS_AND_FACTS_CHUNK_SIZE = 200

    @Bean(name = 'BackoutJob')
    @Override
    Job job() {
        jobs.get(JOB_NAME)
                .start(mainFlow(null, null, null, null))
                .end()
                .build()
    }

    @Bean
    Flow mainFlow(List<BackoutModule> backoutModules,
                  Tasklet gatherCurrentConceptsTasklet,
                  Tasklet validateTopNodePreexistenceTasklet,
                  NextBackoutModuleJobExecutionDecider decider) {

        def flowBuilder = new FlowBuilder<SimpleFlow>('mainFlow')
                .start(allowStartStepOf('gatherCurrentConcepts', gatherCurrentConceptsTasklet))
                .next(stepOf('validateTopNode', validateTopNodePreexistenceTasklet))
                .next(decider)

        backoutModules.each { BackoutModule mod ->
            def presenceStep = wrapStepWithName(
                    "${mod.dataTypeName}.presence",
                    mod.detectPresenceStep())
            def deleteDataStep = wrapStepWithName(
                    "${mod.dataTypeName}.deleteData",
                    mod.deleteDataStep())

            def subFlow = new FlowBuilder<SimpleFlow>("flow.${mod.dataTypeName}")
                    .start(presenceStep)
                    .on(BackoutModule.FOUND.exitCode).to(deleteDataStep)
                    .from(presenceStep).on(ExitStatus.COMPLETED.exitCode).end()
                    .from(presenceStep).on('*').fail().build()

            flowBuilder
                    .on(statusForModule(mod))
                    .to(subFlow)
                    .on(ExitStatus.COMPLETED.exitCode)
                    .to(decider)
        }

        def deciderRecalculateCounts = new StringTemplateBasedDecider(
                "\${jobCtx['${BackoutContext.KEY_CONCEPT_COUNTS_DIRTY_BASE}'] ? " +
                        "'CONTINUE' : 'COMPLETED'}")

        flowBuilder.on(ExitStatus.COMPLETED.exitCode)

                .to(deciderRecalculateCounts)
                .on('CONTINUE')
                .to(stepOf((MethodClosure) this.&deleteConceptCountsTasklet))
                .next(stepOf('insertConceptCounts', insertConceptCountsTasklet(null, null)))

                .from(deciderRecalculateCounts)
                .on(ExitStatus.COMPLETED.exitCode)
                .end()

                .build()
    }

    @Bean
    @StepScopeInterfaced
    Tasklet deleteConceptCountsTasklet(
            @Value("#{jobExecutionContext['conceptCountsDirtyBase']}") ConceptPath topNode) {
        new DeleteConceptCountsTasklet(basePath: topNode)
    }

    @Bean
    @StepScopeInterfaced
    Tasklet insertConceptCountsTasklet(
            DatabaseImplementationClassPicker picker,
            @Value("#{jobExecutionContext['conceptCountsDirtyBase']}") ConceptPath topNode) {
        picker.instantiateCorrectClass(
                OracleInsertConceptCountsTasklet,
                PostgresInsertConceptCountsTasklet).with { InsertConceptCountsTasklet t ->
            basePath = topNode
            t
        }
    }

    @Bean
    @JobScope
    Step deleteConceptsAndFactsStep(
            DeleteFactsWriter deleteFactsWriter,
            DeleteConceptWriter deleteConceptWriter,
            DeleteI2b2TagsWriter deleteI2b2TagsWriter,
            PromoteConceptCountDirtyBaseStepListener promoteConceptCountDirtyBaseStepListener) {

        steps.get('deleteConceptsAndFacts')
                .chunk(DELETE_CONCEPTS_AND_FACTS_CHUNK_SIZE)
                .reader(conceptPathsToDeleteItemReader(null))
                .writer(new CompositeItemWriter(delegates: [
                        deleteFactsWriter,
                        deleteConceptWriter,
                        deleteI2b2TagsWriter,]))
                .listener(new LogCountsStepListener())
                .listener(promoteConceptCountDirtyBaseStepListener)
                .build()
    }

    @Bean
    @StepScope
    IterableItemReader conceptPathsToDeleteItemReader(
            ExpressionResolver expressionResolver
    ) {
        new IterableItemReader(
                name: 'conceptPathsToDelete',
                expressionResolver: expressionResolver,
                expression: '@backoutContext.conceptPathsToDelete')
    }
}

// because we can't repeat @ComponentScan
@ComponentScan([
        'org.transmartproject.batch.backout',
        'org.transmartproject.batch.secureobject',
        'org.transmartproject.batch.biodata',
        'org.transmartproject.batch.concept'])
abstract class BackoutJobConfigurationParent extends AbstractJobConfiguration { }
