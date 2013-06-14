package org.transmartproject.db.highdim

import org.transmartproject.core.dataquery.acgh.Region
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.i2b2data.PatientDimension

class HighDimTestData {

    static DeGplInfo testRegionPlatform = {
        def p = new DeGplInfo(
                title: 'Test Region Platform',
                organism: 'Homo Sapiens',
                annotationDate: Date.parse('yyyy-MM-dd', '2013-05-03'),
                markerTypeId: DeChromosomalRegion.MARKER_TYPE.id,
                genomeBuild: 'genome build #1',
        )
        p.id = 'test-region-platform'
        p
    }()

    static DeGplInfo testBogusTypePlatform = {
        def p = new DeGplInfo(
                markerTypeId: 'bogus marker type',
        )
        p.id = 'bogus-marker-platform'
        p
    }()

    static List<DeChromosomalRegion> testRegions = {
        def r = [
                new DeChromosomalRegion(
                        platform: testRegionPlatform,
                        chromosome: '1',
                        start: 33,
                        end: 9999,
                        numberOfProbes: 42,
                        name: 'region 1:33-9999',
                ),
                new DeChromosomalRegion(
                        platform: testRegionPlatform,
                        chromosome: '2',
                        start: 66,
                        end: 99,
                        numberOfProbes: 2,
                        name: 'region 2:66-99',
                ),
        ]
        r[0].id = -1001L
        r[1].id = -1002L
        r
    }()

    static List<PatientDimension> testRegionPatients =
        [
                [
                        id: -2001,
                        sourcesystemCd: 'REGION_SAMP_TRIAL:SUBJ_ID_1'
                ],
                [
                        id: -2002,
                        sourcesystemCd: 'REGION_SAMP_TRIAL:SUBJ_ID_2'
                ],
        ].collect {
            def p = new PatientDimension(it)
            p.id = it.id
            p
        }

    static List<DeSubjectSampleMapping> testRegionAssays = {
        def patients = testRegionPatients
        def common = [
                siteId: 'site id #1',
                conceptCode: 'concept code #1',
                trialName: 'REGION_SAMP_TRIAL',
                timepointName: 'timepoint name #1',
                timepointCd: 'timepoint code',
                sampleTypeName: 'sample name #1',
                sampleTypeCd: 'sample code',
                tissueTypeName: 'tissue name #1',
                tissueTypeCd: 'tissue code',
                platform: testRegionPlatform,
        ]
        def r = [
                new DeSubjectSampleMapping([
                        patient: patients[0],
                        subjectId: 'SUBJ_ID_1',
                        *:common
                ]),
                new DeSubjectSampleMapping([
                        patient: patients[1],
                        subjectId: 'SUBJ_ID_2',
                        *:common
                ])
        ]
        r[0].id = -3001;
        r[1].id = -3002;
        r
    }()

    static DeSubjectAcghData createACGHData(Region region,
                                            Assay assay,
                                            flag=0) {
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

    static List<DeSubjectAcghData> testACGHData = {
        [
                createACGHData(testRegions[0], testRegionAssays[0], -1),
                createACGHData(testRegions[0], testRegionAssays[1], 0),
                createACGHData(testRegions[1], testRegionAssays[0], 1),
                createACGHData(testRegions[1], testRegionAssays[1], 2),
        ]
    }()

}
