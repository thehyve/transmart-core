package org.transmartproject.db.dataquery.highdim

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.transmartproject.db.i2b2data.PatientDimension

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.isA

class HighDimTestData {

    /* Generic stuff below. The rest is to be moved out once acgh is refactored */

    private static Log LOG = LogFactory.getLog(this)

    static List<DeSubjectSampleMapping> createTestAssays(List<PatientDimension> patients,
                                                         long baseId,
                                                         DeGplInfo platform,
                                                         String trialName = 'SAMP_TRIAL',
                                                         String conceptCode = 'concept code #1', /* bogus */
                                                         String sampleCodePrefix = 'SAMPLE_FOR_') {

        patients.collect { PatientDimension p ->
            def s = new DeSubjectSampleMapping([
                    patient: p,
                    subjectId: p.sourcesystemCd.split(':')[1],

                    /* common */
                    siteId: 'site id #1',
                    conceptCode: conceptCode,
                    trialName: trialName,
                    timepointName: 'timepoint name #1',
                    timepointCd: 'timepoint code',
                    sampleTypeName: 'sample name #1',
                    sampleTypeCd: 'sample code',
                    tissueTypeName: 'tissue name #1',
                    tissueTypeCd: 'tissue code',
                    sampleCode: sampleCodePrefix + p.id,
                    platform: platform,
            ])

            s.id = --baseId
            s
        }
    }

    static List<PatientDimension> createTestPatients(int n, long baseId, String trialName = 'SAMP_TRIAL') {
        (1..n).collect { int i ->
            def p = new PatientDimension(sourcesystemCd: "$trialName:SUBJ_ID_$i")
            p.id = baseId - i
            p
        }
    }

    static void save(List objects) {
        List result = objects*.save()
        result.eachWithIndex { def entry, int i ->
            if (entry == null) {
                LOG.error("Could not save ${objects[i]}. Errors: ${objects[i].errors}")
            }
        }

        assertThat result, everyItem(isA(objects[0].getClass()))
    }
}
