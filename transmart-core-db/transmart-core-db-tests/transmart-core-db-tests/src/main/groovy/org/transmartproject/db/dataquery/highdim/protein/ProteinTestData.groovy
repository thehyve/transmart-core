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

package org.transmartproject.db.dataquery.highdim.protein

import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.i2b2data.PatientDimension

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class ProteinTestData {

    public static final String TRIAL_NAME = 'PROTEIN_SAMP_TRIAL'

    SampleBioMarkerTestData biomarkerTestData = new SampleBioMarkerTestData()

    DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'Bogus protein platform',
                organism: 'Homo Sapiens',
                markerType: 'PROTEOMICS') 
        res.id = 'BOGUS_PROTEIN_PLATFORM'                  // ?? what should be here
        res
    }()

    List<PatientDimension> patients =
        HighDimTestData.createTestPatients(2, -300, TRIAL_NAME)

    List<DeSubjectSampleMapping> assays =
        HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

    List<DeProteinAnnotation> annotations = {
        def createAnnotation = { id, proteinName, uniprotName, peptide ->
            def res = new DeProteinAnnotation(
                    peptide:     peptide,
                    uniprotId:   biomarkerTestData.proteinBioMarkers.find { it.name == proteinName }.externalId,
                    uniprotName: uniprotName,
                    platform:    platform
            )
            res.id = id
            res
        }
        [
                // not the actual full sequences here...
                createAnnotation(-501, 'Adipogenesis regulatory factor', 'PVR_HUMAN1', 'MASKGLQDLK'),
                createAnnotation(-502, 'Adiponectin',                    'PVR_HUMAN2', 'MLLLGAVLLL'),
                createAnnotation(-503, 'Urea transporter 2',             'PVR_HUMAN3', 'MSDPHSSPLL'),
        ]
    }()

    List<DeSubjectProteinData> data = {
        def createDataEntry = { assay, annotation, intensity ->
            new DeSubjectProteinData(
                    assay: assay,
                    annotation: annotation,
                    intensity: intensity,
                    logIntensity: Math.log(intensity),
                    zscore:    (intensity - 0.35) / 0.1871
            )
        }

        def res = []
        Double intensity = 0
        annotations.each { annotation ->
            assays.each { assay ->
                res += createDataEntry assay, annotation, (intensity += 0.1)
            }
        }

        res
    }()

    List<BioMarkerCoreDb> getProteins() {
        biomarkerTestData.proteinBioMarkers
    }

    void saveAll() {
        biomarkerTestData.saveProteinData()

        save([platform])
        save patients
        save assays
        save annotations
        save data
    }
}
