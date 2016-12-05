package org.transmartproject.rest.test

import org.transmartproject.core.dataquery.Sex
import org.transmartproject.db.i2b2data.PatientDimension

class TestData {

    static final long ID = 42L
    static final String TRIAL = 'STUDY_ID_1'
    static final String TRIAL_LC = TRIAL.toLowerCase(Locale.ENGLISH)
    static final String SUBJECT_ID = 'SUBJECT_43'
    static final Date BIRTH_DATE = Date.parseToStringDate('Fri Feb 14 11:44:24 CET 2014')
    static final Sex SEX = Sex.MALE
    static final DEATH_DATE = null
    static final long AGE = 44L
    static final String RACE = 'Caucasian'
    static final String MARITAL_STATUS = 'Married'
    static final String RELIGION = 'Judaism'

    void createTestData() {
        def p = new PatientDimension()
        p.id = ID
        p.sourcesystemCd = "$TRIAL:$SUBJECT_ID"
        p.sexCd = SEX.toString()
        p.birthDate = BIRTH_DATE
        p.deathDate = DEATH_DATE
        p.age = AGE
        p.race = RACE
        p.maritalStatus = MARITAL_STATUS
        p.religion = RELIGION
        p.save()
    }
}
