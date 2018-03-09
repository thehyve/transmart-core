package org.transmartproject.db.ontology

import grails.plugin.cache.CacheEvict
import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cache.annotation.Cacheable
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.user.User as DbUser
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.i2b2data.Study

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap

@Transactional
class MDStudiesService implements MDStudiesResource, ApplicationRunner {

    @Autowired
    UsersResource usersResource

    @Autowired
    AccessControlChecks accessControlChecks

    static private isLegacyStudy(Study study) {
        if (study == null) {
            false
        } else {
            return study.dimensionDescriptions.any { it.name == DimensionDescription.LEGACY_MARKER }
        }
    }

    /**
     * This map is populated with a mapping from study id to study name
     * at application startup.
     */
    final Map<Long, String> studyNumToStudyId = new ConcurrentHashMap<>()

    /**
     * This map is populated with a mapping from study name to study
     * at application startup.
     */
    final Map<String, MDStudy> studyIdToStudy = new ConcurrentHashMap<>()

    /**
     * This map is populated with a mapping from id to study
     * at application startup.
     */
    final SortedMap<Long, MDStudy> idToStudy = new ConcurrentSkipListMap<>()

    /**
     * This map is populated with a mapping from trial visit id to study
     * at application startup.
     */
    final SortedMap<Long, MDStudy> trialVisitIdToStudy = new ConcurrentSkipListMap<>()

    @Override
    void run(ApplicationArguments args) throws Exception {
        log.info "Loading studies ..."
        def t1 = new Date()
        studyNumToStudyId.putAll(Study.findAll().collectEntries { Study it ->
            [(it.id): it.name]
        })
        studyIdToStudy.putAll(Study.findAll().collectEntries { Study it ->
            it.dimensionDescriptions.size()
            [(it.name): it]
        })
        idToStudy.putAll(Study.findAll().collectEntries { Study it ->
            it.dimensionDescriptions.size()
            [(it.id): it]
        })
        trialVisitIdToStudy.putAll(TrialVisit.findAll().collectEntries { TrialVisit it ->
            [(it.id): it.study]
        })
        def t2 = new Date()
        log.info "Done loading studies. (took ${t2.time - t1.time} ms)"
    }


    @Override
    List<MDStudy> getStudies(User currentUser) {
        def user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        accessControlChecks.getDimensionStudiesForUser(user).findAll { !isLegacyStudy(it) }
    }

    @Override
    MDStudy getStudyForUser(Long id, User currentUser) throws NoSuchResourceException {
        def user = usersResource.getUserFromUsername(currentUser.username)
        def study = Study.findById(id)
        if (isLegacyStudy(study)) {
            study = null
        }
        if (study == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, study)) {
            throw new AccessDeniedException("Access denied to study or study does not exist: ${id}")
        }
        study.dimensions.size()
        study
    }

    @Override
    MDStudy getStudyByStudyIdForUser(String studyId, User currentUser) throws NoSuchResourceException {
        def user = usersResource.getUserFromUsername(currentUser.username)
        def study = Study.findByStudyId(studyId)
        if (isLegacyStudy(study)) {
            study = null
        }
        if (study == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, study)) {
            throw new AccessDeniedException("Access denied to study or study does not exist: ${studyId}")
        }
        study.dimensions.size()
        study
    }

    MDStudy getStudyByStudyId(String studyId) {
        MDStudy study = studyIdToStudy[studyId]
        if (!study) {
            study = Study.findByStudyId(studyId)
            if (!study) {
                throw new NoSuchResourceException("Study could not be found: ${studyId}.")
            }
            study.dimensions.size()
            studyIdToStudy[studyId] = study
        }
        study
    }

    MDStudy getStudyById(Long id) {
        MDStudy study = idToStudy[id]
        if (!study) {
            study = Study.findById(id)
            if (!study) {
                throw new NoSuchResourceException("Study could not be found: ${id}.")
            }
            study.dimensions.size()
            idToStudy[id] = study
        }
        study
    }

    MDStudy getStudyByTrialVisitId(Long trialVisitId) {
        MDStudy study = trialVisitIdToStudy[trialVisitId]
        if (!study) {
            study = TrialVisit.findById(trialVisitId)?.study
            if (!study) {
                throw new NoSuchResourceException("Study could not be found for trial visit ${trialVisitId}.")
            }
            study.dimensions.size()
            trialVisitIdToStudy[trialVisitId] = study
        }
        study
    }

    @Override
    @Cacheable('org.transmartproject.db.ontology.MDStudiesService')
    String getStudyIdById(Long id) throws NoSuchResourceException {
        String studyId = studyNumToStudyId[id]
        if (!studyId) {
            def study = Study.findById(id)
            if (!study) {
                throw new NoSuchResourceException("Study could not be found with id ${id}.")
            }
            study.dimensions.size()
            studyId = study.studyId
            studyNumToStudyId[id] = studyId
        }
        studyId
    }

    /**
     * Clear internal caches of the resource.
     */
    @CacheEvict(value = 'org.transmartproject.db.ontology.MDStudiesService', allEntries = true)
    void clearCaches() {
        studyNumToStudyId.clear()
        studyIdToStudy.clear()
        idToStudy.clear()
        trialVisitIdToStudy.clear()
    }

}
