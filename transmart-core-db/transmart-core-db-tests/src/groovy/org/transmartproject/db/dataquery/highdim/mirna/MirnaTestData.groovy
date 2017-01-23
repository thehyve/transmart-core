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

package org.transmartproject.db.dataquery.highdim.mirna

import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.ConceptTestData

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class MirnaTestData {
    
    String typeName
    public static final String TRIAL_NAME = 'MIRNA_SAMP_TRIAL'

    SampleBioMarkerTestData bioMarkerTestData = new SampleBioMarkerTestData()

    DeGplInfo platform 
    List<PatientDimension> patients
    List<DeSubjectSampleMapping> assays
    List<DeQpcrMirnaAnnotation> probes
    List<DeSubjectMirnaData> mirnaData

    ConceptTestData concept = HighDimTestData.createConcept('MIRNAPUBLIC', 'concept code #1', TRIAL_NAME, 'MIRNA_CONCEPT')
    
    public MirnaTestData() {
        generateTestData()
    }

    public MirnaTestData(String typeName) {
        this.typeName = typeName
        generateTestData()
    }
    
    protected void generateTestData() {
        platform = new DeGplInfo(
                title: 'TaqMan® Rodent MicroRNA Array v3.0 A/B',
                organism: 'Mus musculus',
                markerType: typeName == 'mirnaseq' ? 'MIRNA_SEQ' : 'MIRNA_QPCR')
        platform.id = 'BOGUSGPL15466'
        
        patients = HighDimTestData.createTestPatients(2, -300, TRIAL_NAME)
        assays = HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

        probes = createProbes()
        mirnaData = createData()
    }

    protected List<DeQpcrMirnaAnnotation> createProbes() {
        def createAnnotation = { probesetId, mirna, detector ->
            def res = new DeQpcrMirnaAnnotation(
                    mirnaId: mirna,
                    detector: detector,
                    platform: platform
            )
            res.id = probesetId
            res
        }
        [
                createAnnotation(-501, 'hsa-mir-3161', 'mmu-miR-3161-4395373'),
                createAnnotation(-502, null, 'snoRNA135-4380912'),
                createAnnotation(-503, 'hsa-mir-323b', 'mmu-miR-323b-4373305'),
        ]
    }

    protected List<DeSubjectMirnaData> createData() {
        def createMirnaEntry = { DeSubjectSampleMapping assay,
                                      DeQpcrMirnaAnnotation probe,
                                      double intensity ->
            new DeSubjectMirnaData(
                    probe: probe,
                    assay: assay,
                    patient: assay.patient,

                    trialName: TRIAL_NAME,

                    rawIntensity: intensity,
                    logIntensity: Math.log(intensity) / Math.log(2),
                    zscore: intensity * 2, /* non-sensical value */
            )
        }

        def res = []
        Double intensity = 0
        probes.each { probe ->
            assays.each { assay ->
                res += createMirnaEntry assay, probe, (intensity += 0.1)
            }
        }

        res
    }

    void saveAll() {
        bioMarkerTestData.saveMirnaData()

        save([platform])
        save patients
        save assays
        save probes
        save mirnaData

        concept.saveAll()
    }

}
