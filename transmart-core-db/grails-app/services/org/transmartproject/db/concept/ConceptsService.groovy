package org.transmartproject.db.concept

import com.google.common.io.CountingOutputStream
import grails.plugin.cache.Cacheable
import grails.transaction.Transactional
import org.grails.io.support.DevNullPrintStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.concept.Concept
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.TrueConstraint

@Transactional
class ConceptsService implements ConceptsResource, ApplicationRunner {

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    UsersResource usersResource

    /**
     * This map is populated with a mapping from concept paths to concept codes
     * at application startup, as an effective way to work around a curiosity in the
     * i2b2 data model: that the concept path is the primary key, but the concept
     * code is used as reference in the observation_fact table instead.
     */
    HashMap<String, String> conceptPathToConceptCode

    @Override
    void run(ApplicationArguments args) throws Exception {
        log.info "Loading concept codes ..."
        def t1 = new Date()
        conceptPathToConceptCode = ConceptDimension.findAll().collectEntries {
            [(it.conceptPath): it.conceptCode]
        } as HashMap
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
        org.transmartproject.db.user.User dbUser = (org.transmartproject.db.user.User) usersResource.getUserFromUsername(user.username)
        if (dbUser.admin) {
            return ConceptDimension.findAll()
        } else {
            return multiDimService.getDimensionElements(multiDimService.getDimension('concept'), new TrueConstraint(), dbUser).asList()
        }
    }

    @Override
    Concept getConceptByConceptCodeForUser(String conceptCode, User user) throws NoSuchResourceException {
        org.transmartproject.db.user.User dbUser = (org.transmartproject.db.user.User) usersResource.getUserFromUsername(user.username)
        Concept concept = null
        if (dbUser.admin) {
            concept = ConceptDimension.findByConceptCode(conceptCode)
        } else {
            def constraint = new ConceptConstraint(conceptCode: conceptCode)
            Iterable<Concept> concepts = multiDimService.getDimensionElements(multiDimService.getDimension('concept'), constraint, dbUser)
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

    @Override
    Concept getConceptByConceptCode(String conceptCode) throws NoSuchResourceException {
        Concept result = ConceptDimension.findByConceptCode(conceptCode)
        if (!result) {
            throw new NoSuchResourceException("No concept with code '${conceptCode}' could be found.")
        }
        result
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
        String code = null
        if (conceptPathToConceptCode) {
            code = conceptPathToConceptCode[conceptPath]
        }
        if (!code) {
            code = getConceptByConceptPath(conceptPath).conceptCode
        }
        code
    }

}
