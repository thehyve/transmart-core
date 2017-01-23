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

package org.transmartproject.db.dataquery.highdim.metabolite

import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.search.SearchKeywordCoreDb

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class MetaboliteTestData {
    public static final String TRIAL_NAME = 'METABOLITE_EXAMPLE_TRIAL'

    SampleBioMarkerTestData biomarkerTestData = new SampleBioMarkerTestData()

    ConceptTestData concept = HighDimTestData.createConcept('METABOLITEPUBLIC', 'concept code #1', TRIAL_NAME, 'METABOLITE_CONCEPT')

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
        def ret = [ /* keep in sync with SampleBioMarkerTestData */
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

    List<DeMetaboliteSubPathway> subPathways = {
        def id = -700
        def createSubPathway = { String name,
                                 DeMetaboliteSuperPathway superPathway ->
            def ret = new DeMetaboliteSubPathway(
                    name: name,
                    superPathway: superPathway,
                    gplId: platform)
            ret.id = --id
            ret
        }

        [ /* keep in sync with SampleBioMarkerTestData */
                createSubPathway('No superpathway subpathway', null),
                createSubPathway('Cholesterol biosynthesis', superPathways[0]),
                createSubPathway('Squalene synthesis', superPathways[0]),
                createSubPathway('Pentose Metabolism', superPathways[1]),
        ]
    }()

    List<SearchKeywordCoreDb> searchKeywordsForSubPathways = {
        def baseId = -800
        subPathways.collect {
            def res = new SearchKeywordCoreDb(
                    keyword: it.name,
                    bioDataId: it.id,
                    uniqueId: "METABOLITE_SUBPATHWAY:$it.id", /* no actual external pk */
                    dataCategory: 'METABOLITE_SUBPATHWAY',
            )
            res.id = --baseId
            res
        }
    }()

    List<SearchKeywordCoreDb> searchKeywordsForSuperPathways = {
        def baseId = -900
        superPathways.collect {
            def res = new SearchKeywordCoreDb(
                    keyword: it.name,
                    bioDataId: it.id,
                    uniqueId: "METABOLITE_SUPERPATHWAY:$it.id", /* no actual external pk */
                    dataCategory: 'METABOLITE_SUPERPATHWAY',
            )
            res.id = --baseId
            res
        }
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
        [ /* keep in sync with SampleBioMarkerTestData */
                createAnnotation(-501, 'Cryptoxanthin epoxide', 'HMDB30538', []),
                createAnnotation(-502, 'Cryptoxanthin 5,6:5\',8\'-diepoxide', 'HMDB30537', subPathways[0..1]),
                createAnnotation(-503, 'Majoroside F4', 'HMDB30536', subPathways[1..3]),
        ]
    }()

    List<DeSubjectMetabolomicsData> data = {
        def createDataEntry = { assay, annotation, intensity ->
            new DeSubjectMetabolomicsData(
                    assay: assay,
                    patient: assay.patient,
                    annotation: annotation,
                    rawIntensity: intensity,
                    logIntensity: Math.log(intensity),
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
        save searchKeywordsForSuperPathways
        save subPathways
        save searchKeywordsForSubPathways
        save annotations
        save data
        concept.saveAll()
    }
}
