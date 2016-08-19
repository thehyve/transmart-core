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

package org.transmartproject.db.dataquery.highdim.rnaseqcog

import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.i2b2data.PatientDimension

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class RnaSeqCogTestData {

    public static final String TRIAL_NAME = 'RNASEQ_COG_SAMP_TRIAL'

    SampleBioMarkerTestData biomarkerTestData = new SampleBioMarkerTestData()

    DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'Bogus RNA-Seq platform',
                organism: 'Homo Sapiens',
                markerType: 'RNASEQ')
        res.id = 'BOGUS_RNA-SEQ_PLATFORM'                  // ?? what should be here
        res
    }()

    List<PatientDimension> patients =
        HighDimTestData.createTestPatients(2, -300, TRIAL_NAME)

    List<DeSubjectSampleMapping> assays =
        HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

    List<DeRnaseqAnnotation> annotations = {
        def createAnnotation = { id, BioMarkerCoreDb gene ->
            def res = new DeRnaseqAnnotation(
                    geneSymbol:   gene.name,
                    geneId:       gene.externalId,
                    platform:     platform
            )
            res.id = id
            res
        }

        long id = -500L

        biomarkerTestData.geneBioMarkers[0..2].collect {
            createAnnotation(--id as String, it)
        }
    }()

    List<DeSubjectRnaData> data = {
        def createDataEntry = { assay, annotation, intensity ->
            new DeSubjectRnaData(
                    assay: assay,
                    annotation:   annotation,
                    rawIntensity: intensity,
                    logIntensity: Math.log(intensity),
                    zscore:       (intensity - 0.35) / 0.1871
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

    List<BioMarkerCoreDb> getGenes() {
        biomarkerTestData.geneBioMarkers
    }

    void saveAll() {
        biomarkerTestData.saveGeneData()

        save([platform])
        save patients
        save assays
        save annotations
        save data
    }
}
