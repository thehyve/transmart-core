package org.transmartproject.rest.data

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.dataquery.Sex
import org.transmartproject.db.Dictionaries
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.user.AccessLevelTestData

import java.time.Instant

@CompileStatic
@Slf4j
class V1DefaultTestData extends TestData {

    @Override
    @Transactional
    void createTestData() {
        log.info "Setup test dictionaries."
        new Dictionaries().saveAll()
        log.info "Create test data."
        def testData = org.transmartproject.db.TestData.createDefault()
        testData.saveAll()
        createExtraPatient()
        def accessLevelTestData = AccessLevelTestData.createWithAlternativeConceptData(testData.conceptData)
        accessLevelTestData.saveAll()
    }

    private static void createExtraPatient() {
        def p = new PatientDimension()
        p.id = 42L
        p.sourcesystemCd = "STUDY_ID_1:SUBJECT_43"
        p.sexCd = Sex.MALE
        p.birthDate = Date.from(Instant.parse('2014-02-14T11:44:24Z'))
        //Date.parseToStringDate('Fri Feb 14 11:44:24 CET 2014')
        p.deathDate = null
        p.age = 44L
        p.race = 'Caucasian'
        p.maritalStatus = 'Married'
        p.religion = 'Judaism'
        p.save(flush: true, failOnError: true)
    }
}
