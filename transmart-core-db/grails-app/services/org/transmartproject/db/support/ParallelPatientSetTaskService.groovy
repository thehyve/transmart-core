package org.transmartproject.db.support

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jsr166y.ForkJoinPool
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.users.User
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.Combination
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

import static groovyx.gpars.GParsPool.withExistingPool

@Slf4j
class ParallelPatientSetTaskService {

    @Autowired
    MultiDimensionalDataResource multiDimensionalDataResource

    @Autowired
    SystemResource systemResource

    @Autowired
    PatientSetResource patientSetResource

    @Autowired
    ForkJoinPool workerPool


    @Canonical
    @CompileStatic
    static class TaskParameters {
        Constraint constraint
        User user
    }

    @Canonical
    @CompileStatic
    static class SubtaskParameters {
        int task
        Constraint constraint
        User user
    }

    @Canonical
    @CompileStatic
    static class ConstraintParts {
        PatientSetConstraint patientSetConstraint
        Constraint otherConstraint
    }

    @CompileStatic
    static ConstraintParts getConstraintParts(Constraint constraint) {
        PatientSetConstraint patientSetConstraint = null
        Constraint otherConstraint = new TrueConstraint()
        if (constraint instanceof PatientSetConstraint) {
            patientSetConstraint = (PatientSetConstraint)constraint
        } else if (constraint instanceof Combination && ((Combination)constraint).operator == Operator.AND) {
            def groups = ((Combination)constraint).args.split { it instanceof PatientSetConstraint }
            def patientSetConstraints = groups[0] as List<PatientSetConstraint>
            def otherConjuncts = groups[1] as List<Constraint>
            if (patientSetConstraints.size() == 1) {
                patientSetConstraint = patientSetConstraints[0]
                otherConstraint = new AndConstraint(otherConjuncts).normalise()
            }
        }
        new ConstraintParts(patientSetConstraint, otherConstraint)
    }

    /**
     * Splits a constraint into a patient set subconstraint and other subconstraint,
     * and splits up the task into multiple parallel tasks that are applied to subsets of
     * the patient set. When all are completed, the combine task is called.
     * For parallelisation, the constraint needs to be a conjunction of a {@link PatientSetConstraint} and other constraints,
     * or a singleton {@link PatientSetConstraint}.
     * If the constraint cannot be split in this way, a single subtask is executed.
     *
     * @param constraint the constraint on which to apply the task.
     * @param user
     * @param subTask the subtask implementation.
     * @param combine the function that combines the results of the subtasks.
     * @return the result of the combination of subtasks, or of the sequential fallback.
     */
    public <ResultType, SubtaskResultEntry> ResultType run(
            TaskParameters parameters,
            Function<SubtaskParameters, List<SubtaskResultEntry>> subTask,
            Function<List<SubtaskResultEntry>, ResultType> combine
            ) {
        log.info "Start parallel task ..."
        def constraintParts = getConstraintParts(parameters.constraint)
        if (!constraintParts.patientSetConstraint || !constraintParts.patientSetConstraint.patientSetId) {
            log.info "Cannot parallelise, using sequential version instead ..."
            def taskParameters = new SubtaskParameters(1, parameters.constraint, parameters.user)
            List<SubtaskResultEntry> results = subTask.apply(taskParameters)
            return combine.apply(results)
        }

        def t1 = new Date()

        def patientSet = patientSetResource.findQueryResult(
                constraintParts.patientSetConstraint.patientSetId, parameters.user)
        long size = patientSet.setSize
        int workers = systemResource.runtimeConfig.numberOfWorkers
        int chunkSize = systemResource.runtimeConfig.patientSetChunkSize
        int numTasks = Math.ceil((size / chunkSize).doubleValue()).intValue()
        log.info "Patient set has ${size} patients. Preparing ${numTasks} tasks of size ${chunkSize} for ${workers} workers..."

        final results = [] as List<SubtaskResultEntry>
        final syncResults = Collections.synchronizedList(results)
        final error = new AtomicBoolean(false)
        final numCompleted = new AtomicInteger(0)
        if (numTasks) {
            withExistingPool(workerPool) {
                (1..numTasks).eachParallel { int i ->
                    try {
                        int offset = chunkSize * (i - 1)
                        log.debug "Starting subtask ${i} (offset: ${offset}) ..."
                        def patientSubsetConstraint = new PatientSetConstraint(
                                patientSetId: constraintParts.patientSetConstraint.patientSetId,
                                offset: offset,
                                limit: chunkSize
                        )
                        def taskConstraint = new AndConstraint([patientSubsetConstraint, constraintParts.otherConstraint]).normalise()
                        def subtaskParameters = new SubtaskParameters(i, taskConstraint, parameters.user)
                        List<SubtaskResultEntry> taskResult = subTask.apply(subtaskParameters)
                        log.debug "Task ${i} done. (${syncResults.size()}"
                        if (taskResult != null && !taskResult.empty) {
                            syncResults.addAll(taskResult)
                        }
                        log.debug "Task ${i}: results added."
                    } catch (Throwable e) {
                        error.set(true)
                        log.error "Error in task ${i}: ${e.message}", e
                    }
                    log.info "${numCompleted.incrementAndGet()} / ${numTasks} tasks completed."
                }
            }
        }
        def t2 = new Date()
        if (error.get()) {
            log.error "Task failed after ${t2.time - t1.time} ms."
            throw new UnexpectedResultException('Task failed.')
        }
        log.info "All ${numTasks} tasks completed. (took ${t2.time - t1.time} ms.)"
        log.info "Combining subtask results ..."
        combine.apply(results)
    }

}
