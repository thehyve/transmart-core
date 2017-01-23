package org.transmartproject.batch.highdim.datastd

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.transmartproject.batch.highdim.assays.AssayMappingsRowStore
import org.transmartproject.batch.highdim.assays.MappingFileRow
import org.transmartproject.batch.patient.Patient
import org.transmartproject.batch.patient.PatientSet

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasProperty

/**
 * Test... {@link PatientInjectionProcessor}
 */
class PatientInjectionProcessorTests {

    static final List PATIENTS = [
            new Patient(id: 'subj1', code: 1),
            new Patient(id: 'subj2', code: 2),
    ]

    static final List MAPPING_FILE_ROWS = [
            new MappingFileRow(sampleCd: 'sampl11', subjectId: 'subj1'),
            new MappingFileRow(sampleCd: 'sampl21', subjectId: 'subj2'),
    ]

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException exception = ExpectedException.none()

    @Test
    void testBasicSuccessfulScenario() {
        PatientInjectionSupport result = initAndRunWith(MAPPING_FILE_ROWS, PATIENTS, 'sampl21')

        assertThat result,
                hasProperty('patient',
                        hasProperty('code', equalTo(2L))
                )
    }

    @Test
    void testNoMappingRow() {
        exception.expect(IllegalStateException)
        exception.expectMessage('No subject found for the sample: sampl21')

        initAndRunWith([], PATIENTS, 'sampl21')
    }

    @Test
    void testNoPatient() {
        exception.expect(IllegalStateException)
        exception.expectMessage('No patient with the following subject id found: subj2')

        initAndRunWith(MAPPING_FILE_ROWS, [], 'sampl21')
    }

    private initAndRunWith(List<MappingFileRow> mappingFileRows, List<Patient> patients, String sampleCode) {
        //init
        def assayMappings = new AssayMappingsRowStore()
        mappingFileRows.each { assayMappings << it }
        def patientSet = new PatientSet()
        patients.each { patientSet << it }
        def testee = new PatientInjectionProcessor(
                assayMappings: assayMappings,
                patientSet: patientSet
        )
        def patientInjectionSupportTestBean = new PatientInjectionSupportTestBean(
                patient: null,
                sampleCode: sampleCode
        )
        //run
        testee.process(patientInjectionSupportTestBean)

        patientInjectionSupportTestBean
    }

    class PatientInjectionSupportTestBean implements PatientInjectionSupport {
        Patient patient
        String sampleCode
    }

}
