package org.transmartproject.db.dataquery.highdim.metabolite

import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.i2b2data.PatientDimension

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class MetaboliteTestData {
    public static final String TRIAL_NAME = 'METABOLITE_EXAMPLE_TRIAL'

    //SampleBioMarkerTestData biomarkerTestData = new SampleBioMarkerTestData()

    DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'Bogus metabolite platform',
                organism: 'Homo Sapiens',
                markerType: 'METABOLOMICS')
        res.id = 'BOGUS_METABOLITE_PLATFORM'
        res
    }()

    List<PatientDimension> patients =
        HighDimTestData.createTestPatients(2, -300, TRIAL_NAME)

    List<DeSubjectSampleMapping> assays =
        HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

    List<DeMetaboliteAnnotation> annotations = {
        def createAnnotation = { id, metaboliteName, metabolite ->
            def res = new DeMetaboliteAnnotation(
                    metabolite:   metabolite,
                    //uniprotId: biomarkerTestData.metaboliteBioMarkers.find { it.name == metaboliteName }.primaryExternalId,
                    platform:  platform
            )
            res.id = id
            res
        }
        [
                // not the actual full sequences here...
                createAnnotation(-501, 'Adipogenesis regulatory factor', 'MASKGLQDLK'),
                createAnnotation(-502, 'Adiponectin',                    'MLLLGAVLLL'),
                createAnnotation(-503, 'Urea transporter 2',             'MSDPHSSPLL'),
        ]
    }()

    List<DeSubjectMetabolomicsData> data = {
        def createDataEntry = { assay, annotation, intensity ->
            new DeSubjectMetabolomicsData(
                    assay: assay,
                    annotation: annotation,
                    intensity: intensity,
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

    void saveAll() {
        //biomarkerTestData.saveMetaboliteData()

        save([platform])
        save(patients)
        save(assays)
        save(annotations)
        save(data)
    }
}