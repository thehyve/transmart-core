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

    DeGplInfo platform2 = {
        def res = new DeGplInfo(
                title: 'Another platform',
                organism: 'Homo Sapiens',
                markerTypeId: 'Gene Expression')
        res.id = 'BOGUSANNOTH'
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
            createI2b2(level: 1, fullName: '\\foo\\xpto2\\', name: 'xpto2'),
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
                ),
                new ConceptDimension(
                        conceptPath: '\\foo\\xpto2\\',
                        conceptCode: 'CODE-XPTO2'
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
                    'SAMPLE_TRIAL_2', dimensionConcepts[1].conceptCode) +
            HighDimTestData.createTestAssays(patients, -500, platform2,
                     'SAMPLE_TRIAL_1', dimensionConcepts[2].conceptCode)
    }()

    void saveAll() {
        save patients
        save i2b2TopConcepts
        save i2b2GenericConcepts
        save dimensionConcepts
        save assays
    }
}
