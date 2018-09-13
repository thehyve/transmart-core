package org.transmartproject.db.concept

import com.google.common.io.CountingOutputStream
import grails.transaction.Transactional
import groovy.transform.Memoized
import org.grails.io.support.DevNullPrintStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cache.annotation.Cacheable
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.concept.Concept
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.User
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint

import java.util.concurrent.ConcurrentHashMap

@Transactional
class ConceptsService implements ConceptsResource, ApplicationRunner {

    @Autowired
    MultiDimensionalDataResource multiDimService

    /**
     * This map is populated with a mapping from concept paths to concept codes
     * at application startup, as an effective way to work around a curiosity in the
     * i2b2 data model: that the concept path is the primary key, but the concept
     * code is used as reference in the observation_fact table instead.
     */
    final Map<String, String> conceptPathToConceptCode = new ConcurrentHashMap<>()

    /**
     * This map is populated with a mapping from concept paths to concept codes
     * at application startup, as an effective way to work around a curiosity in the
     * i2b2 data model: that the concept path is the primary key, but the concept
     * code is used as reference in the observation_fact table instead.
     */
    final Map<String, Concept> conceptCodeToConcept = new ConcurrentHashMap<>()

    @Override
    void run(ApplicationArguments args) throws Exception {
        log.info "Loading concept codes ..."
        def t1 = new Date()
        ConceptDimension.findAll().each { ConceptDimension it ->
            conceptPathToConceptCode[it.conceptPath] = it.conceptCode
            conceptCodeToConcept[it.conceptCode] = it
        }
        OutputStream counter = new CountingOutputStream(new DevNullPrintStream())
        counter.withObjectOutputStream { s ->
            s.writeObject(conceptPathToConceptCode)
            s.flush()
        }
        def t2 = new Date()
        log.info "Done loading concept codes. (took ${t2.time - t1.time} ms, occupies ~${counter.count} bytes)"
    }

    @Override
    List<Concept> getConcepts(User user) {
        if (user.admin) {
            return ConceptDimension.findAll()
        } else {
            return multiDimService.getDimensionElements(multiDimService.getDimension('concept'), new TrueConstraint(), user).asList()
        }
    }

    @Override
    Concept getConceptByConceptCodeForUser(String conceptCode, User user) throws NoSuchResourceException {
        Concept concept = null
        if (user.admin) {
            concept = ConceptDimension.findByConceptCode(conceptCode)
        } else {
            def constraint = new ConceptConstraint(conceptCode: conceptCode)
            Iterable<Concept> concepts = multiDimService.getDimensionElements(multiDimService.getDimension('concept'), constraint, user)
            def iterator = concepts.iterator()
            if (iterator.hasNext()) {
                concept = iterator.next()
            }
        }
        if (!concept) {
            throw new NoSuchResourceException("No concept with code '${conceptCode}' could be found.")
        }
        concept
    }

    @Memoized
    Concept getConceptByConceptCode(String conceptCode) throws NoSuchResourceException {
        Concept concept = conceptCodeToConcept[conceptCode]
        if (!concept) {
            concept = ConceptDimension.findByConceptCode(conceptCode)
            if (!concept) {
                throw new NoSuchResourceException("No concept with code '${conceptCode}' could be found.")
            }
            conceptCodeToConcept[conceptCode] = concept
        }
        concept
    }

    @Override
    Concept getConceptByConceptPath(String conceptPath) throws NoSuchResourceException {
        Concept result = ConceptDimension.findByConceptPath(conceptPath)
        if (!result) {
            throw new NoSuchResourceException("No concept with conceptPath '${conceptPath}' could be found.")
        }
        result
    }

    @Override
    @Cacheable('org.transmartproject.db.concept.ConceptsService')
    String getConceptCodeByConceptPath(String conceptPath) throws NoSuchResourceException {
        String code = conceptPathToConceptCode[conceptPath]
        if (!code) {
            code = getConceptByConceptPath(conceptPath).conceptCode
            if (!code) {
                throw new NoSuchResourceException("No concept with path '${conceptPath}' could be found.")
            }
            conceptPathToConceptCode[conceptPath] = code
        }
        code
    }

}
