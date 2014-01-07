package org.transmartproject.db.dataquery.highdim

import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.ontology.TableAccess

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
import static org.transmartproject.db.ontology.ConceptTestData.createI2b2
import static org.transmartproject.db.ontology.ConceptTestData.createTableAccess

class AssayTestData {

    DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'Affymetrix Human Genome U133A 2.0 Array',
                organism: 'Homo Sapiens',
                markerTypeId: 'Gene Expression')
        res.id = 'BOGUSGPL570'
        res
    }()

    List<PatientDimension> patients = HighDimTestData.createTestPatients(3, -100)

    List<TableAccess> i2b2TopConcepts = [
            createTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                    tableCode: 'i2b2 main', tableName: 'i2b2'),
    ]

    List<I2b2> i2b2GenericConcepts = [
            createI2b2(level: 1, fullName: '\\foo\\bar\\', name: 'bar'),
            createI2b2(level: 1, fullName: '\\foo\\xpto\\', name: 'xpto'),
    ]

    List<ConceptDimension> dimensionConcepts = {
        [
                new ConceptDimension(
                        conceptPath: '\\foo\\bar\\',
                        conceptCode: 'CODE-BAR'
                ),
                new ConceptDimension(
                        conceptPath: '\\foo\\xpto\\',
                        conceptCode: 'CODE-XPTO'
                )
        ]
    }()

    List<DeSubjectSampleMapping> assays = {
        //save is cascaded to the platform
        HighDimTestData.createTestAssays(patients, -200, platform,
                'SAMPLE_TRIAL_1', dimensionConcepts[0].conceptCode) +
            HighDimTestData.createTestAssays(patients, -300, platform,
                    'SAMPLE_TRIAL_1', dimensionConcepts[1].conceptCode) +
            HighDimTestData.createTestAssays(patients, -400, platform,
                    'SAMPLE_TRIAL_2', dimensionConcepts[1].conceptCode)
    }()

    void saveAll() {
        save patients
        save i2b2TopConcepts
        save i2b2GenericConcepts
        save dimensionConcepts
        save assays
    }
}
