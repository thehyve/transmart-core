package org.transmartproject.db.concept

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.user.User
import spock.lang.Specification

@Rollback
@Integration
class ConceptServiceSpec extends Specification {

    @Autowired
    ConceptsResource conceptsResource

    @Autowired
    UsersResource usersResource

    void "test fetching all concepts"() {
        def user = User.findByUsername('test-public-user-1')

        when: "fetching all concepts for a regular user"
        def result = conceptsResource.getConcepts(user)

        then: "all concept codes of public data should be returned"
        result*.conceptCode.containsAll(
            'VSIGN:HR, VSIGN:TEMP, HD:LUNG:BIOPSY, CV:DEM:AGE, CV:DEM:SEX:M, CV:DEM:SEX:F, CV:DEM:RACE, CT:DEM:AGE'
            .split(', ')
        )
    }

    void "test fetching all concepts for admin"() {
        def admin = User.findByUsername('admin')
        def user = User.findByUsername('test-public-user-1')

        when: "fetching all concepts for admin"
        def adminResult = conceptsResource.getConcepts(admin)
        def userResult = conceptsResource.getConcepts(user)
        def diff = adminResult*.conceptCode - userResult*.conceptCode

        then: "the result should also contain concept codes of private data"
        !diff.empty
        diff.contains('SCSCP:DEM:AGE')
    }

    void "test fetching a concept"() {
        def user = User.findByUsername('test-public-user-1')

        when: "fetching concept by code VSIGN:HR"
        def result = conceptsResource.getConceptByConceptCode('VSIGN:HR')

        then: "the heart rate concept should be returned"
        result.conceptCode == 'VSIGN:HR'
        result.name == 'Heart Rate'
    }

    void "test fetching a concept with only private data for regular user"() {
        def user = User.findByUsername('test-public-user-1')

        when: "fetching concept by code SCSCP:DEM:AGE"
        def result = conceptsResource.getConceptByConceptCodeForUser('SCSCP:DEM:AGE', user)

        then: "access is denied"
        thrown(AccessDeniedException)
    }

    void "test fetching a concept with only private data for admin"() {
        def user = User.findByUsername('admin')

        when: "fetching concept by code SCSCP:DEM:AGE"
        def result = conceptsResource.getConceptByConceptCodeForUser('SCSCP:DEM:AGE', user)

        then: "the age concept should be returned"
        result.conceptCode == 'SCSCP:DEM:AGE'
        result.name == 'Age'
    }

    void "test fetching concept by concept path"() {
        when: "fetching the concept by concept path"
        def result = conceptsResource.getConceptByConceptPath('\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\')

        then: "the correct concept is returned"
        result.name == 'Age'
        result.conceptCode == 'SCSCP:DEM:AGE'
    }

    void "test fetching concept code by concept path"() {
        when: "fetching the concept code by concept path"
        def conceptCode = conceptsResource.getConceptCodeByConceptPath('\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\')

        then: "the correct code is returned"
        conceptCode == 'SCSCP:DEM:AGE'
    }

}
