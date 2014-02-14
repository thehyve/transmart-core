package org.transmartproject.rest.marshallers

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.junit.Test
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.Sex

import java.text.DateFormat
import java.text.SimpleDateFormat

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue

class PatientMarshallerTests {


    private static final long ID               = 42L
    private static final String TRIAL          = 'TEST_STUDY'
    private static final String SUBJECT_ID     = 'SUBJECT_43'
    private static final Date BIRTH_DATE       = Date.parseToStringDate('Fri Feb 14 11:44:24 CET 2014')
    private static final Sex SEX               = Sex.MALE
    private static final DEATH_DATE            = null
    private static final long AGE              = 44L
    private static final String RACE           = 'Caucasian'
    private static final String MARITAL_STATUS = 'Married'
    private static final String RELIGION       = 'Judaism'

    Patient getMockPatient() {
        [
                getId:            { -> ID },
                getTrial:         { -> TRIAL },
                getInTrialId:     { -> SUBJECT_ID },
                getSex:           { -> SEX },
                getBirthDate:     { -> BIRTH_DATE },
                getDeathDate:     { -> DEATH_DATE },
                getAge:           { -> AGE },
                getRace:          { -> RACE },
                getMaritalStatus: { -> MARITAL_STATUS },
                getReligion:      { -> RELIGION },
        ] as Patient
    }

    @Test
    void basicTest() {
        def json = mockPatient as JSON

        JsonSlurper slurper = new JsonSlurper()
        println json.toString()
        println slurper.parseText(json.toString())
        assertThat slurper.parseText(json.toString()), allOf(
                hasEntry('id',            ID as Integer),
                hasEntry('trial',         TRIAL),
                hasEntry('inTrialId',     SUBJECT_ID),
                hasEntry('birthDate',     formatAsISO(BIRTH_DATE)),
                hasEntry('sex',           SEX.name()),
                hasEntry(is('deathDate'), is(nullValue())),
                hasEntry('age',           AGE as Integer),
                hasEntry('race',          RACE),
                hasEntry('maritalStatus', MARITAL_STATUS),
                hasEntry('religion',      RELIGION))
    }

    private static String formatAsISO(Date date) {
        TimeZone tz = TimeZone.getTimeZone 'UTC'
        DateFormat df = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss\'Z\'')
        df.timeZone = tz
        df.format date
    }

}
