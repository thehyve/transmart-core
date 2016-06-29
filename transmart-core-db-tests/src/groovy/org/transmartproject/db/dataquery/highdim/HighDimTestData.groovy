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

package org.transmartproject.db.dataquery.highdim

import org.transmartproject.db.TestDataHelper
import org.transmartproject.db.biomarker.BioDataCorrelDescr
import org.transmartproject.db.biomarker.BioDataCorrelationCoreDb
import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationType
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.search.SearchGeneSignature
import org.transmartproject.db.search.SearchKeywordCoreDb

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class HighDimTestData {

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

    //to be removed (unnecessary indirection)
    static List<PatientDimension> createTestPatients(int n, long baseId, String trialName = 'SAMP_TRIAL') {
        I2b2Data.createTestPatients(n, baseId, trialName)
    }

    /* returns list with two elements: the biomarkers, and the search keywords */
    static List<BioMarkerCoreDb> createBioMarkers(long baseId,
                                                  List<Map<String, String>> attributes,
                                                  String type = 'GENE',
                                                  String organism = 'HOMO SAPIENS',
                                                  String sourceCode = 'Entrez') {
        (0..attributes.size() - 1).collect { int i ->
            assertThat([ attributes[i].name,
                    attributes[i].externalId ], everyItem(is(notNullValue())))
            def bm = new BioMarkerCoreDb(
                    type: type,
                    organism: organism,
                    sourceCode: sourceCode,
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
                    uniqueId: "$it.type:$it.externalId",
                    dataCategory: it.type,
            )
            res.id = --baseId
            res
        }
    }

    static List<SearchKeywordCoreDb> createSearchKeywordsForGeneSignatures(
           List<SearchGeneSignature> geneSignatures, long baseId) {
        geneSignatures.collect { sig ->
            def res = new SearchKeywordCoreDb(
                    keyword: sig.name,
                    bioDataId: sig.id,
                    uniqueId: "GENESIG:$sig.id",
                    dataCategory: 'GENESIG',
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


    static List<BioDataCorrelationCoreDb> createCorrelationPairs(
            long baseId, List<BioMarkerCoreDb> from, List<BioMarkerCoreDb> to) {

        def createCorrelation = { long id,
                                  BioMarkerCoreDb left,
                                  BioMarkerCoreDb right ->

            // registryTable's rows are the target, hence the order of the arg
            CorrelationType correlationType = CORRELATION_TYPES_REGISTRY.
                    registryTable.get(right.type, left.type)
            if (correlationType == null) {
                throw new RuntimeException("Didn't know I could associate " +
                        "$left.type with $right.type")
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
            createCorrelation baseId - 1 - i, from[i], to[i]
        }
    }

    //to be removed (unnecessary indirection)
    static void save(List objects) {
        TestDataHelper.save(objects)
    }

}
