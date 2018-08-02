package org.transmartproject.db.support

import grails.util.Holders
import groovy.transform.CompileStatic
import org.grails.core.util.StopWatch
import org.hibernate.SessionFactory
import org.modelmapper.ModelMapper
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.config.CompletionStatus
import org.transmartproject.core.config.RuntimeConfig
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.config.UpdateStatus
import org.transmartproject.core.exceptions.ServiceNotAvailableException
import org.transmartproject.core.userquery.UserQuerySetResource
import org.transmartproject.db.clinical.AggregateDataOptimisationsService
import org.transmartproject.db.clinical.AggregateDataService
import org.transmartproject.db.clinical.PatientSetService
import org.transmartproject.db.config.RuntimeConfigImpl
import org.transmartproject.core.config.RuntimeConfigRepresentation
import org.transmartproject.db.ontology.MDStudiesService
import org.transmartproject.db.ontology.OntologyTermTagsResourceService
import org.transmartproject.db.ontology.TrialVisitsService
import org.transmartproject.db.tree.TreeCacheService
import org.transmartproject.db.tree.TreeService
import org.transmartproject.db.util.SharedLock

import javax.validation.Valid

import static grails.async.Promises.task

@CompileStatic
class SystemService implements SystemResource {

    private final int DEFAULT_PATIENT_SET_CHUNK_SIZE = 10000

    private final RuntimeConfigImpl runtimeConfig = new RuntimeConfigImpl(
            Holders.config.getProperty('org.transmartproject.system.numberOfWorkers', Integer.class, Runtime.getRuntime().availableProcessors()),
            Holders.config.getProperty('org.transmartproject.system.patientSetChunkSize', Integer.class, DEFAULT_PATIENT_SET_CHUNK_SIZE)
    )

    private final ModelMapper modelMapper = new ModelMapper()

    @Autowired
    AggregateDataService aggregateDataService

    @Autowired
    TreeService treeService

    @Autowired
    TreeCacheService treeCacheService

    @Autowired
    OntologyTermTagsResourceService ontologyTermTagsResourceService

    @Autowired
    MDStudiesService studiesService

    @Autowired
    TrialVisitsService trialVisitsService

    @Autowired
    UserQuerySetResource userQuerySetResource

    @Autowired
    AggregateDataOptimisationsService aggregateDataOptimisationsService

    @Autowired
    PatientSetService patientSetService

    @Autowired
    SessionFactory sessionFactory


    RuntimeConfig getRuntimeConfig() {
        return modelMapper.map(runtimeConfig, RuntimeConfigRepresentation.class)
    }

    RuntimeConfig updateRuntimeConfig(@Valid RuntimeConfig config) {
        runtimeConfig.setNumberOfWorkers(config.numberOfWorkers)
        runtimeConfig.setPatientSetChunkSize(config.patientSetChunkSize)
        getRuntimeConfig()
    }

    static final private SharedLock lock = new SharedLock()

    static final updateStatusLock = new Object()

    UpdateStatus updateStatus = null

    void changeUpdateStatus(CompletionStatus status, String message = null) {
        synchronized (updateStatusLock) {
            updateStatus.status = status
            if (message) {
                updateStatus.message = message
            }
            updateStatus.updateDate = new Date()
        }
    }

    /**
     * Clears the tree node cache, the tags cache, the counts caches and the studies caches.
     * This function should be called after loading, removing or updating
     * tree nodes or observations in the database.
     */
    void clearCaches() {
        treeCacheService.clearAllCacheEntries()
        ontologyTermTagsResourceService.clearTagsCache()
        aggregateDataService.clearCountsCache()
        aggregateDataService.clearCountsPerStudyAndConceptCache()
        studiesService.clearCaches()
        trialVisitsService.clearCache()
    }

    Map<String, Runnable> updateTasks = [
            'clear caches': { ->
                clearCaches() } as Runnable,
            'refresh study concept bitset materialized view': { ->
                aggregateDataOptimisationsService.clearPatientSetBitset() } as Runnable,
            'clear patient sets': { ->
                patientSetService.clearPatientSets() } as Runnable,
            'user query set scan': { ->
                userQuerySetResource.scan() } as Runnable,
            'rebuild caches': { ->
                treeService.rebuildCacheTask() } as Runnable
    ]

    void updateAfterDataLoadingTask() {
        def session = null
        try {
            def stopWatch = new StopWatch('Update after data loading')
            session = sessionFactory.openSession()
            changeUpdateStatus(CompletionStatus.RUNNING)
            // Execute tasks
            for (Map.Entry<String, Runnable> task: updateTasks.entrySet()) {
                synchronized (updateStatusLock) {
                    updateStatus.tasks[task.key] = CompletionStatus.RUNNING
                }
                stopWatch.start(task.key)
                log.info "Starting task: ${task.key} ..."
                task.value.run()
                stopWatch.stop()
                synchronized (updateStatusLock) {
                    updateStatus.tasks[task.key] = CompletionStatus.COMPLETED
                }
            }
            log.info "Done updating after data loading.\n${stopWatch.prettyPrint()}"
            changeUpdateStatus(CompletionStatus.COMPLETED)
        } catch (Throwable e) {
            def message = e.message ?: e.cause?.message
            log.error "Unexpected error while updating: ${message}", e
            changeUpdateStatus(CompletionStatus.FAILED, message)
            throw e
        } finally {
            log.debug "Closing task (lock: ${lock.locked})"
            session?.close()
            lock.unlock()
            log.debug "Task closed (lock: ${lock.locked})"
        }
    }

    /**
     * Clears the caches, patient sets, refreshes a materialized view with study_concept bitset
     * and scans for the changes for subscribed user queries.
     * @param currentUser
     */
    UpdateStatus updateAfterDataLoading() {
        if (!lock.tryLock()) {
            throw new ServiceNotAvailableException('Update operation already in progress.')
        }
        synchronized (updateStatusLock) {
            def now = new Date()
            def tasks = [:] as Map<String, CompletionStatus>
            for (String taskName: updateTasks.keySet()) {
                tasks[taskName] = CompletionStatus.CREATED
            }
            updateStatus = new UpdateStatus(CompletionStatus.CREATED, tasks, now, now, null)
        }
        log.info "Update after data loading started."
        task {
            updateAfterDataLoadingTask()
        }
        synchronized (updateStatusLock) {
            return updateStatus
        }
    }

    /**
     * Checks if a cache rebuild task is active.
     * Only available for administrators.
     *
     * @param currentUser the current user.
     * @return true iff a cache rebuild task is active.
     */
    UpdateStatus getUpdateStatus() {
        synchronized (updateStatusLock) {
            return updateStatus
        }
    }

}
