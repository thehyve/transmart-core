/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db

import grails.util.Holders
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.exception.GenericJDBCException
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.db.arvados.ArvadosTestData
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.storage.StorageTestData

class TestData {

    ConceptTestData conceptData
    I2b2Data i2b2Data
    I2b2Data secondI2b2Data // yeah...
    ClinicalTestData clinicalData
    MrnaTestData mrnaData
    AcghTestData acghData
    SampleBioMarkerTestData bioMarkerTestData
    StorageTestData storageTestData
    ArvadosTestData arvadosTestData

    static void reset() {
        ClinicalTestData.reset()
    }

    static TestData createDefault() {
        def conceptData = ConceptTestData.createDefault()
        def i2b2Data = I2b2Data.createDefault() // study 1

        def study2Patients = I2b2Data.createTestPatients 2, -200, 'STUDY_ID_2'
        def i2b2DataStudy2 = new I2b2Data(
                trialName: 'STUDY_ID_2',
                patients: study2Patients,
                patientTrials: I2b2Data.createPatientTrialLinks(study2Patients, 'STUDY_ID_2'))
        def extraFacts = ClinicalTestData.createDiagonalCategoricalFacts(
                2,
                [conceptData.i2b2List.find { it.name == 'male' }, // on study 2
                 conceptData.i2b2List.find { it.name == 'female' }],
                study2Patients)

        def clinicalData = ClinicalTestData.createDefault(conceptData.i2b2List, i2b2Data.patients)

        clinicalData.facts += extraFacts

        def bioMarkerTestData = new SampleBioMarkerTestData()
        def mrnaData = new MrnaTestData('2', bioMarkerTestData) //concept code '2'
        def acghData = new AcghTestData('4', bioMarkerTestData) //concept code '4'
        def storageTestData = StorageTestData.createDefault()
        def arvadosTestData = ArvadosTestData.createDefault()

        new TestData(
                conceptData: conceptData,
                i2b2Data: i2b2Data,
                secondI2b2Data: i2b2DataStudy2,
                clinicalData: clinicalData,
                mrnaData: mrnaData,
                acghData: acghData,
                bioMarkerTestData: bioMarkerTestData,
                storageTestData: storageTestData,
                arvadosTestData: arvadosTestData,
        )
    }

    private static I2b2 createI2b2(Map props) {
        ConceptTestData.createI2b2([code: props['name'], *: props])
    }

    /**
     * This testdata is partially copied from ClinicalDataRetrievalSpec. Having separate 'default' TestDatas for
     * tabular and hypercube data is not desirable, so these should be merged, but I had some problems with that when
     * I tried it.
     *
     * TODO: have a single default TestData that includes both hypercube and tabular data.
     */
    static TestData createHypercubeDefault() {
        def tableAccess = ConceptTestData.createTableAccess(
                level: 0,
                fullName: '\\foo\\',
                name: 'foo',
                tableCode: 'i2b2 main',
                tableName: 'i2b2')

        def i2b2List = [
                createI2b2(level: 1, fullName: '\\foo\\concept 1\\', name: 'd1'), //not c, to test ordering
                createI2b2(level: 1, fullName: '\\foo\\concept 2\\', name: 'c2', cVisualattributes: 'LA'),
                createI2b2(level: 1, fullName: '\\foo\\concept 3\\', name: 'c3'),
                createI2b2(level: 1, fullName: '\\foo\\concept 4\\', name: 'c4'),
                createI2b2(level: 1, fullName: '\\foo\\concept 5\\', name: 'c5'),
                createI2b2(level: 1, fullName: '\\foo\\concept 6\\', name: 'c6'),
                createI2b2(level: 1, fullName: '\\foo\\concept 7\\', name: 'c7'),
        ]

        def conceptDims = ConceptTestData.createConceptDimensions(i2b2List)

        List<Patient> patients = I2b2Data.createTestPatients(3, -100, 'SAMP_TRIAL')

        def conceptData = new ConceptTestData(tableAccesses: [tableAccess], i2b2List: i2b2List, conceptDimensions: conceptDims)

        def i2b2Data = new I2b2Data(trialName: 'TEST', patients: patients)

        def clinicalData = ClinicalTestData.createHypercubeDefault(conceptDims, patients)

        new TestData(conceptData: conceptData, i2b2Data: i2b2Data, clinicalData: clinicalData)
    }


    void saveAll() {
        conceptData?.saveAll()
        i2b2Data?.saveAll()
        secondI2b2Data?.saveAll()
        clinicalData?.saveAll()
        bioMarkerTestData?.saveAll()
        mrnaData?.saveAll()
        mrnaData?.updateDoubleScaledValues()
        acghData?.saveAll()
        storageTestData?.saveAll()
        arvadosTestData?.saveAll()
    }

    @Lazy
    static List<String> allTables = {
        SessionFactory sessionFactory = Holders.applicationContext.getBean(SessionFactory)
        List<Class> domainClasses = Holders.grailsApplication.getArtefacts("Domain")*.clazz
        List<String> tables = domainClasses.collect { sessionFactory.getClassMetadata(it).tableName }

        tables
    }()

    /**
     * Clear data, but only within the current transaction. When this transaction ends (e.g. at the end of the
     * current test case), the original data is visible again.
     * Note that if your test makes a rest api call, the call will be handled in a different thread and a different
     * transaction, so in that case this method won't help you.
     */
    static void clearAllDataInTransaction() {
        clearAllData(true)
    }

    /**
     * Clear data globally, visible over all connections and transactions. You will need to restore any data manually
     * after calling this.
     * @param currentTransactionOnly false. When true, deletes only happen in the current transaction, but then you
     * should use clearAllDataInTransaction() instead.
     */
    static void clearAllData(boolean currentTransactionOnly = false) {
        reset()

        Session session = Holders.applicationContext.getBean(SessionFactory).currentSession
        session.createSQLQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate()

        allTables.each {
            try {
                session.createSQLQuery(currentTransactionOnly ? "DELETE FROM $it;" : "TRUNCATE TABLE $it;").executeUpdate()
            } catch (GenericJDBCException e) {
                //ignore CANNOT TRUNCATE xxx, as several domain classes are backed by views
            }
        }
        session.createSQLQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate()
        session.clear()
        //session.flush()

        SystemResource systemResource = Holders.applicationContext.getBean(SystemResource)
        systemResource.clearCaches()
    }

}
