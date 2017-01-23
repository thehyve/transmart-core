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

package org.transmartproject.db.dataquery.highdim.acgh

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.chromoregion.Region
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.dataquery.highdim.chromoregion.DeChromosomalRegion
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.search.SearchKeywordCoreDb

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.*
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult

class AcghTestData {

    static final String TRIAL_NAME = 'REGION_SAMP_TRIAL'

    static final String ACGH_PLATFORM_MARKER_TYPE = 'Chromosomal'

    SampleBioMarkerTestData bioMarkerTestData

    private String conceptCode

    AcghTestData(String conceptCode = 'concept code #1',
                 SampleBioMarkerTestData bioMarkerTestData = null) {
        this.conceptCode = conceptCode
        this.bioMarkerTestData = bioMarkerTestData ?: new SampleBioMarkerTestData()
    }

    @Lazy List<SearchKeywordCoreDb> searchKeywords = {
        bioMarkerTestData.geneSearchKeywords +
                bioMarkerTestData.proteinSearchKeywords +
                bioMarkerTestData.geneSignatureSearchKeywords
    }()

    DeGplInfo regionPlatform = {
        def p = new DeGplInfo(
                title: 'Test Region Platform',
                organism: 'Homo Sapiens',
                annotationDate: Date.parse('yyyy-MM-dd', '2013-05-03'),
                markerType: ACGH_PLATFORM_MARKER_TYPE,
                genomeReleaseId: 'hg18',
        )
        p.id = 'test-region-platform'
        p
    }()

    DeGplInfo bogusTypePlatform = {
        def p = new DeGplInfo(
                markerTypeId: 'bogus marker type',
        )
        p.id = 'bogus-marker-platform'
        p
    }()

    List<DeChromosomalRegion> regions = {
        def r = [
                new DeChromosomalRegion(
                        platform: regionPlatform,
                        chromosome: '1',
                        start: 33,
                        end: 9999,
                        numberOfProbes: 42,
                        name: 'region 1:33-9999',
                        cytoband: 'cytoband1',
                        geneSymbol: 'ADIRF',
                        geneId: -130753
                ),
                new DeChromosomalRegion(
                        platform: regionPlatform,
                        chromosome: '2',
                        start: 66,
                        end: 99,
                        numberOfProbes: 2,
                        name: 'region 2:66-99',
                        cytoband: 'cytoband2',
                        geneSymbol: 'AURKA',
                        geneId: -130751
                ),
        ]
        r[0].id = -1001L
        r[1].id = -1002L
        r
    }()

    List<PatientDimension> patients = createTestPatients(2, -2000, 'REGION_SAMP_TRIAL')

    QtQueryMaster allPatientsQueryResult = createQueryResult(patients)

    List<DeSubjectSampleMapping> assays = createTestAssays(patients,
                                                           -3000L,
                                                           regionPlatform,
                                                           TRIAL_NAME,
                                                           conceptCode)

    DeSubjectAcghData createACGHData(Region region,
                                     Assay assay,
                                     flag = 0) {
        new DeSubjectAcghData(
                region:                     region,
                assay:                      assay,
                patient:                    assay.patient,
                chipCopyNumberValue:        0.11d,
                segmentCopyNumberValue:     0.12d,
                flag:                       flag,
                probabilityOfLoss:          0.11d + (flag == -1 ? 0.08d : 0),
                probabilityOfNormal:        0.13d + (flag == 0 ? 0.08d : 0),
                probabilityOfGain:          0.14d + (flag == 1 ? 0.08d : 0),
                probabilityOfAmplification: 0.15d + (flag == 2 ? 0.08d : 0),
        )
    }

    List<DeSubjectAcghData> acghData = {
        [
                createACGHData(regions[0], assays[0], -1),
                createACGHData(regions[0], assays[1], 0),
                createACGHData(regions[1], assays[0], 1),
                createACGHData(regions[1], assays[1], 2),
        ]
    }()

    void saveAll() {
        bioMarkerTestData.saveGeneData()

        save([ regionPlatform, bogusTypePlatform ])
        save regions
        save patients
        save([ allPatientsQueryResult ])
        save assays
        save acghData
    }

}
