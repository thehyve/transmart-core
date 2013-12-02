package org.transmartproject.db.dataquery.highdim.mrna

import org.transmartproject.db.bioassay.BioAssayDataAnnotationCoreDb
import org.transmartproject.db.bioassay.BioAssayFeatureGroupCoreDb
import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.search.SearchGeneSignature
import org.transmartproject.db.search.SearchGeneSignatureItem
import org.transmartproject.db.search.SearchKeywordCoreDb
import org.transmartproject.db.user.SearchAuthPrincipal
import org.transmartproject.db.user.SearchAuthUser

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createSearchKeywordsForBioMarkers
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class MrnaTestData {

    public static final String TRIAL_NAME = 'MRNA_SAMP_TRIAL'

    SampleBioMarkerTestData bioMarkerTestData = new SampleBioMarkerTestData()

    DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'Affymetrix Human Genome U133A 2.0 Array',
                organism: 'Homo Sapiens',
                markerTypeId: 'Gene Expression')
        res.id = 'BOGUSGPL570'
        res
    }()

    List<BioMarkerCoreDb> getBioMarkers() {
        bioMarkerTestData.geneBioMarkers
    }

    List<SearchKeywordCoreDb> searchKeywords = {
        bioMarkerTestData.geneSearchKeywords +
                bioMarkerTestData.proteinSearchKeywords +
                bioMarkerTestData.geneSignatureSearchKeywords
    }()

    List<DeMrnaAnnotationCoreDb> annotations = {
        def createAnnotation = { probesetId, probeId, BioMarkerCoreDb bioMarker ->
            def res = new DeMrnaAnnotationCoreDb(
                    gplId: platform.id,
                    probeId: probeId,
                    geneSymbol: bioMarker.name,
                    geneId: bioMarker.primaryExternalId,
                    organism: 'Homo sapiens',
            )
            res.id = probesetId
            res
        }
        [
                createAnnotation(-201, '1553506_at', bioMarkers[0]),
                createAnnotation(-202, '1553510_s_at', bioMarkers[1]),
                createAnnotation(-203, '1553510_s_at', bioMarkers[2]),
        ]
    }()

    List<PatientDimension> patients =
        HighDimTestData.createTestPatients(2, -300, TRIAL_NAME)

    List<DeSubjectSampleMapping> assays =
        HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

    List<DeSubjectMicroarrayDataCoreDb> microarrayData = {
        def common = [
                trialName: TRIAL_NAME,
                //trialSource: "$TRIAL_NAME:STD" (not mapped)
        ]
        def createMicroarrayEntry = { DeSubjectSampleMapping assay,
                                      DeMrnaAnnotationCoreDb probe,
                                      double intensity ->
            new DeSubjectMicroarrayDataCoreDb(
                    probe: probe,
                    assay: assay,
                    patient: assay.patient,
                    rawIntensity: intensity,
                    logIntensity: Math.log(intensity) / Math.log(2),
                    zscore: intensity * 2, /* non-sensical value */
                    *: common,
            )
        }

        def res = []
        Double intensity = 0
        annotations.each { probe ->
            assays.each { assay ->
                res += createMicroarrayEntry assay, probe, (intensity += 0.1)
            }
        }

        res
    }()


    void saveAll() {
        bioMarkerTestData.saveGeneData()

        assertThat platform.save(), is(notNullValue(DeGplInfo))
        save annotations
        save patients
        save assays
        save microarrayData
    }
}
