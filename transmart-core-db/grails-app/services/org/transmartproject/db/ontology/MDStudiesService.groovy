package org.transmartproject.db.ontology

import grails.plugin.cache.CacheEvict
import grails.transaction.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cache.annotation.Cacheable
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.AuthorisationHelper
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.metadata.DimensionDescription

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.stream.Collectors

@Transactional
@CompileStatic
class MDStudiesService implements MDStudiesResource, ApplicationRunner {

    @Autowired
    AuthorisationChecks authorisationChecks

    @Autowired
    SessionFactory sessionFactory

    static private boolean isLegacyStudy(MDStudy study) {
        if (study == null || !(study instanceof Study)) {
            false
        } else {
            ((Study)study).dimensionDescriptions?.any { it.name == DimensionDescription.LEGACY_MARKER } ?: false
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
    List<MDStudy> getStudiesWithMinimalPatientDataAccessLevel(User user, PatientDataAccessLevel minimalPatientDataAccessLevel) {
        List<MDStudy> studies
        if (user.admin) {
            studies = Study.findAll() as List<MDStudy>
        } else {
            def accessibleStudyTokens = AuthorisationHelper.getStudyTokensForUserWithMinimalPatientDataAccessLevel(user, minimalPatientDataAccessLevel)
            def criteria = DetachedCriteria.forClass(Study)
            criteria.add(Restrictions.in('secureObjectToken', accessibleStudyTokens))
            studies = criteria.getExecutableCriteria(sessionFactory.currentSession).list() as List<MDStudy>
        }
        studies.stream()
                .filter({MDStudy study -> !isLegacyStudy(study) })
                .collect(Collectors.toList())
    }

    @Override
    List<MDStudy> getStudiesWithPatientDataAccessLevel(User user, PatientDataAccessLevel patientDataAccessLevel) {
        def studyTokens = AuthorisationHelper.getStudyTokensForUserWithPatientDataAccessLevel(user, patientDataAccessLevel)
        if (!studyTokens) {
            return []
        }
        def criteria = DetachedCriteria.forClass(Study)
        criteria.add(Restrictions.in('secureObjectToken', studyTokens))
        criteria.getExecutableCriteria(sessionFactory.currentSession)
                .list().stream()
                .filter({ MDStudy study -> !isLegacyStudy(study) })
                .collect(Collectors.toList())
    }

    @CompileDynamic
    @Override
    MDStudy getStudyForUser(Long id, User currentUser) throws NoSuchResourceException {
        def study = Study.findById(id)
        checkAccessToStudy(study, currentUser, id)
        study
    }

    @CompileDynamic
    @Override
    MDStudy getStudyByStudyIdForUser(String studyId, User currentUser) throws NoSuchResourceException {
        def study = Study.findByStudyId(studyId)
        checkAccessToStudy(study, currentUser, studyId)
        study
    }

    @CompileDynamic
    @Override
    List<MDStudy> getStudiesByStudyIdsForUser(List<String> studyIds, User currentUser) throws NoSuchResourceException {
        def studies = Study.findAllByStudyIdInList(studyIds)
        def studiesForUser = []
        for (study in studies) {
            try {
                checkAccessToStudy(study, currentUser, study?.studyId)
                studiesForUser.add(study)
            } catch (NoSuchResourceException e) {
                // ignore here
            }
        }
        if(studiesForUser.size() == 0) {
            throw new NoSuchResourceException("No study found for specified studyIds.")
        }
        studiesForUser
    }

    private void checkAccessToStudy(Study study, User user, id) {
        if (isLegacyStudy(study)) {
            study = null
        }
        if (study == null || !authorisationChecks.hasAnyAccess(user, study)) {
            throw new NoSuchResourceException("Access denied to study or study does not exist: ${id}")
        }
        fixLoad(study)
    }

    private static Study fixLoad(Study study) {
        // Ensure study dimensions are loaded in the same hibernate session as the study object itself. This is
        // needed for parallel query processing where each parallel thread has its own hibernate session.
        study.dimensions.size()
        study
    }

    @CompileDynamic
    MDStudy getStudyByStudyId(String studyId) {
        MDStudy study = studyIdToStudy[studyId]
        if (!study) {
            study = Study.findByStudyId(studyId)
            if (!study) {
                throw new NoSuchResourceException("Study could not be found: ${studyId}.")
            }
            fixLoad(study)
            studyIdToStudy[studyId] = study
        }
        study
    }

    @CompileDynamic
    MDStudy getStudyById(Long id) {
        MDStudy study = idToStudy[id]
        if (!study) {
            study = Study.findById(id)
            if (!study) {
                throw new NoSuchResourceException("Study could not be found: ${id}.")
            }
            fixLoad(study)
            idToStudy[id] = study
        }
        study
    }

    @CompileDynamic
    @Override
    @Cacheable('org.transmartproject.db.ontology.MDStudiesService')
    String getStudyIdById(Long id) throws NoSuchResourceException {
        String studyId = studyNumToStudyId[id]
        if (!studyId) {
            def study = Study.findById(id)
            if (!study) {
                throw new NoSuchResourceException("Study could not be found with id ${id}.")
            }
            fixLoad(study)
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
