package org.transmartproject.db.dataquery.highdim

import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.ontology.TableAccess

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
import static org.transmartproject.db.ontology.ConceptTestData.createI2b2
import static org.transmartproject.db.ontology.ConceptTestData.createTableAccess

class AssayTestData {

    static List<PatientDimension> patients = HighDimTestData.createTestPatients(3, -100)

    static List<TableAccess> i2b2TopConcepts = [
            createTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                    tableCode: 'i2b2 main', tableName: 'i2b2'),
    ]

    static List<I2b2> i2b2GenericConcepts = [
            createI2b2(level: 1, fullName: '\\foo\\bar\\', name: 'bar'),
            createI2b2(level: 1, fullName: '\\foo\\xpto\\', name: 'xpto'),
    ]

    static List<ConceptDimension> dimensionConcepts = {
        [
                new ConceptDimension(
                        conceptPath: '\\foo\\bar\\',
                        conceptCd: 'CODE-BAR'
                ),
                new ConceptDimension(
                        conceptPath: '\\foo\\xpto\\',
                        conceptCd: 'CODE-XPTO'
                )
        ]
    }()

    static List<DeSubjectSampleMapping> assays = {
        //save is cascaded to the platform
        HighDimTestData.createTestAssays(patients, -200, MrnaTestData.platform,
                'SAMPLE_TRIAL_1', dimensionConcepts[0].conceptCd) +
            HighDimTestData.createTestAssays(patients, -300, MrnaTestData.platform,
                    'SAMPLE_TRIAL_1', dimensionConcepts[1].conceptCd) +
            HighDimTestData.createTestAssays(patients, -400, MrnaTestData.platform,
                    'SAMPLE_TRIAL_2', dimensionConcepts[1].conceptCd)
    }()

    static void saveAll() {
        save patients
        save i2b2TopConcepts
        save i2b2GenericConcepts
        save dimensionConcepts
        save assays
    }
}
