package org.transmartproject.db.dataquery.highdim.mirna

import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.PatientDimension

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class MirnaTestData {

    public static final String TRIAL_NAME = 'MIRNA_SAMP_TRIAL'

    static DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'TaqMan® Rodent MicroRNA Array v3.0 A/B',
                organism: 'Mus musculus',
                markerTypeId: 'QPCR MIRNA')
        res.id = 'BOGUSGPL15466'
        res
    }()

    static List<PatientDimension> patients =
        HighDimTestData.createTestPatients(2, -300, TRIAL_NAME)

    static List<DeSubjectSampleMapping> assays =
        HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

    static List<DeQpcrMirnaAnnotation> probes = {
        def createAnnotation = { probesetId, mirna, detector ->
            def res = new DeQpcrMirnaAnnotation(
                    mirnaId: mirna,
                    detector: detector,
            )
            res.id = probesetId
            res
        }
        [
                createAnnotation(-501, 'mmu-miR-147', 'mmu-miR-147-4395373'),
                createAnnotation(-502, null, 'snoRNA135-4380912'),
                createAnnotation(-503, 'mmu-miR-153', 'mmu-miR-153-4373305'),
        ]
    }()

    static List<DeSubjectMirnaData> mirnaData = {
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
    }()

    void saveAll() {
        save([ platform ])
        save patients
        save assays
        save probes
        save mirnaData
    }

}
