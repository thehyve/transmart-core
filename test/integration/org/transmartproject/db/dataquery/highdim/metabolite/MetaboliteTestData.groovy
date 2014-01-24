package org.transmartproject.db.dataquery.highdim.metabolite

import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.i2b2data.PatientDimension

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class MetaboliteTestData {
    public static final String TRIAL_NAME = 'METABOLITE_EXAMPLE_TRIAL'

    SampleBioMarkerTestData biomarkerTestData = new SampleBioMarkerTestData()

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

    List<DeMetaboliteSuperPathway> superPathways = {
        def ret = [
                new DeMetaboliteSuperPathway(
                        name: 'Carboxylic Acid',
                        gplId: platform),
                new DeMetaboliteSuperPathway(
                        name: 'Phosphoric Acid',
                        gplId: platform),
        ]
        ret[0].id = -601
        ret[1].id = -602
        ret
    }()

    List<DeMetaboliteSubPathway> subpathways = {
        def id = -600
        def createSubPathway = { String name,
                                 DeMetaboliteSuperPathway superPathway ->
            def ret = new DeMetaboliteSubPathway(
                    name: name,
                    superPathway: superPathway,
                    gplId: platform)
            ret.id = --id
            ret
        }

        [
                createSubPathway('No superpathway subpathway', null),
                createSubPathway('Cholesterol biosynthesis', superPathways[0]),
                createSubPathway('Squalene synthesis', superPathways[0]),
                createSubPathway('Pentose Metabolism', superPathways[1]),
        ]
    }()

    List<DeMetaboliteAnnotation> annotations = {
        def createAnnotation = { id,
                                 metaboliteName,
                                 metabolite,
                                 List<DeMetaboliteSubPathway> subpathways ->
            def res = new DeMetaboliteAnnotation(
                    biochemicalName: metaboliteName,
                    hmdbId:          metabolite,
                    platform:        platform
            )
            subpathways.each {
                it.addToAnnotations(res)
            }
            res.id = id
            res
        }
        [
                createAnnotation(-501, 'Cryptoxanthin epoxide', 'HMDB30538', []),
                createAnnotation(-502, 'Cryptoxanthin 5,6:5\',8\'-diepoxide', 'HMDB30537', subpathways[0..1]),
                createAnnotation(-503, 'Majoroside F4', 'HMDB30536', subpathways[1..3]),
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
        biomarkerTestData.saveMetabolomicsData()

        save([platform])
        save patients
        save assays
        save superPathways
        save subpathways
        save annotations
        save data
    }
}
