package org.transmartproject.db.dataquery.highdim

import org.transmartproject.db.i2b2data.PatientDimension

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.*
/**
 * Sample, generic high dimensional test data, not bound to any specific
 * data type.
 */
class SampleHighDimTestData {

    public static final String TRIAL_NAME = 'GENERIC_SAMPLE_TRIAL'

    DeGplInfo platform = {
        def p = new DeGplInfo(
                title: 'Test Generic Platform',
                organism: 'Homo Sapiens',
                markerType: 'generic',
                annotationDate: Date.parse('yyyy-MM-dd', '2013-05-03'),
                genomeReleaseId: 'hg18',
        )
        p.id = 'test-generic-platform'
        p
    }()

    List<PatientDimension> patients = createTestPatients(2, -2000, TRIAL_NAME)

    List<DeSubjectSampleMapping> assays = createTestAssays(
            patients, -3000L, platform, TRIAL_NAME)

    void saveAll() {
        save([ platform ])
        save patients
        save assays
    }

}
