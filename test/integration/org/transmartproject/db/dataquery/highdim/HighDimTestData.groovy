package org.transmartproject.db.dataquery.highdim

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.transmartproject.db.biomarker.BioDataCorrelDescr
import org.transmartproject.db.biomarker.BioDataCorrelationCoreDb
import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationType
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.search.SearchKeywordCoreDb

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

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
                    patientInTrialId: p.sourcesystemCd.split(':')[1],

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

    /* returns list with two elements: the biomarkers, and the search keywords */
    static List<BioMarkerCoreDb> createBioMarkers(long baseId,
                                                  List<Map<String, String>> attributes,
                                                  String type = 'GENE',
                                                  String organism = 'HOMO SAPIENS',
                                                  String primarySourceCode = 'Entrez') {
        (0..attributes.size() - 1).collect { int i ->
            assertThat([ attributes[i].name,
                    attributes[i].primaryExternalId ], everyItem(is(notNullValue())))
            def bm = new BioMarkerCoreDb(
                    type: type,
                    organism: organism,
                    primarySourceCode: primarySourceCode,
                    *:attributes[i])
            bm.id = baseId - 1 - i
            bm
        }
    }

    static List<SearchKeywordCoreDb> createSearchKeywordsForBioMarkers(
            List<BioMarkerCoreDb> biomarkers, long baseId) {
        biomarkers.collect { BioMarkerCoreDb it ->
            def res = new SearchKeywordCoreDb(
                    keyword: it.name,
                    bioDataId: it.id,
                    uniqueId: "$it.type:$it.primaryExternalId",
                    dataCategory: $it.type,
            )
            res.id = --baseId
            res
        }
    }

    static CorrelationTypesRegistry CORRELATION_TYPES_REGISTRY = {
        def registry = new CorrelationTypesRegistry()
        registry.init()
        registry
    }()

    static long BIO_DATA_CORREL_DESCR_SEQ = -9100L


    static List<BioDataCorrelationCoreDb> createCorrelationPair(
            long baseId, List<BioMarkerCoreDb> from, List<BioMarkerCoreDb> to) {

        def createCorrelation = { long id,
                                  BioMarkerCoreDb left,
                                  BioMarkerCoreDb right ->

            CorrelationType correlationType = CORRELATION_TYPES_REGISTRY.
                    registryTable.get(from.type, to.type)
            if (correlationType == null) {
                throw new RuntimeException("Didn't know I could associate " +
                        "$from.type with $to.type")
            }

            BioDataCorrelDescr descr =
                BioDataCorrelDescr.findByCorrelation(correlationType.name)
            if (!descr) {
                descr = new BioDataCorrelDescr(
                        correlation: correlationType.name,
                        /* the rest doesn't really matter */
                )
                descr.id = --BIO_DATA_CORREL_DESCR_SEQ
            }

            def res = new BioDataCorrelationCoreDb(
                    description:    descr,
                    leftBioMarker:  left,
                    rightBioMarker: right)
            res.id = id
            res
        }

        (0..from.size() - 1).collect { i ->
            createCorrelation baseId - 1 - i, from[0], to[0]
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
