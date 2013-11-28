package org.transmartproject.db.dataquery.highdim.rbm

import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.PatientDimension

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class RbmTestData {

    public static final String TRIAL_NAME = 'RBM_SAMP_TRIAL'

    static DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'RBM platform',
                organism: 'Homo Sapiens',
                markerTypeId: 'RBM')
        res.id = 'BOGUSRBMplatform'
        res
    }()

    static List<PatientDimension> patients =
        HighDimTestData.createTestPatients(2, -300, TRIAL_NAME)

    static List<DeSubjectSampleMapping> assays =
        HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

    static List<DeRbmAnnotation> deRbmAnnotations = {
        def createAnnotation = { id, antigene, uniprotId, geneSymbol, geneId ->
            def res = new DeRbmAnnotation(
                    gplId: platform.id,
                    antigenName: antigene,
                    uniprotId: uniprotId,
                    geneSymbol: geneSymbol,
                    geneId: geneId
            )
            res.id = id
            res
        }
        [
                createAnnotation(-501, 'Adiponectin', 'Q15848', 'AURKA', -601),
                createAnnotation(-502, 'Urea transporter 2', 'Q15849', 'SLC14A2', -602),
                createAnnotation(-503, 'Adipogenesis regulatory factor', 'Q15847', 'ADIRF', -603),
        ]
    }()

    static List<DeSubjectRbmData> rbmData = {
        def createMirnaEntry = { DeSubjectSampleMapping assay,
                                 DeRbmAnnotation deRbmAnnotation,
                                      double intensity ->
            new DeSubjectRbmData(
                    trialName: TRIAL_NAME,
                    assay: assay,

                    deRbmAnnotation: deRbmAnnotation,
                    patient: assay.patient,

                    logIntensity: Math.log(intensity) / Math.log(2),
                    zscore: intensity * 2,
            )
        }

        def res = []
        Double intensity = 0
        deRbmAnnotations.each { deRbmAnnotation ->
            assays.each { assay ->
                res += createMirnaEntry assay, deRbmAnnotation, (intensity += 0.1)
            }
        }

        res
    }()

    void saveAll() {
        save([ platform ])
        save patients
        save assays
        save deRbmAnnotations
        save rbmData
    }

}
