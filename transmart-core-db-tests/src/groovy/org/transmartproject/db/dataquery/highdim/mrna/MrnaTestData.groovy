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

package org.transmartproject.db.dataquery.highdim.mrna

import grails.util.Holders
import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.search.SearchKeywordCoreDb

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class MrnaTestData {

    public static final String TRIAL_NAME = 'MRNA_SAMP_TRIAL'

    SampleBioMarkerTestData bioMarkerTestData

    private String conceptCode

    DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'Affymetrix Human Genome U133A 2.0 Array',
                organism: 'Homo Sapiens',
                markerType: 'Gene Expression',
                genomeReleaseId: 'hg19')
        res.id = 'BOGUSGPL570'
        res
    }()

    MrnaTestData(String conceptCode = 'concept code #1',
                 SampleBioMarkerTestData bioMarkerTestData = null) {
        this.conceptCode = conceptCode
        this.bioMarkerTestData = bioMarkerTestData ?: new SampleBioMarkerTestData()
    }

    List<BioMarkerCoreDb> getBioMarkers() {
        bioMarkerTestData.geneBioMarkers
    }

    @Lazy List<SearchKeywordCoreDb> searchKeywords = {
        bioMarkerTestData.geneSearchKeywords +
                bioMarkerTestData.proteinSearchKeywords +
                bioMarkerTestData.geneSignatureSearchKeywords
    }()

    @Lazy List<DeMrnaAnnotationCoreDb> annotations = {
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
                createAnnotation(-203, '1553513_at', bioMarkers[2]),
        ]
    }()

    List<PatientDimension> patients =
        HighDimTestData.createTestPatients(2, -300, TRIAL_NAME)

    @Lazy List<DeSubjectSampleMapping> assays = {
        HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME, conceptCode) }()

    @Lazy List<DeSubjectMicroarrayDataCoreDb> microarrayData = {
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
        //doubles loose some precision when adding 0.1, so i use BigDecimals instead
        BigDecimal intensity = BigDecimal.ZERO
        annotations.each { probe ->
            assays.each { assay ->
                intensity = intensity + 0.1
                res += createMicroarrayEntry assay, probe, intensity
            }
        }

        res
    }()

    void saveAll(boolean skipBioMarkerData = false) {
        if (!skipBioMarkerData) {
            bioMarkerTestData.saveGeneData()
        }

        assertThat platform.save(), is(notNullValue(DeGplInfo))
        save annotations
        save patients
        save assays
        save microarrayData
    }

    void updateDoubleScaledValues() {
        //making sure BigDecimals use the scale specified in the db (otherwise toString() will yield different results)
        Holders.applicationContext.sessionFactory.currentSession.flush()
        microarrayData.each { it.refresh() }
    }
}
