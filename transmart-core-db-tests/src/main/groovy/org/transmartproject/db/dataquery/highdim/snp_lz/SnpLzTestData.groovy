/*
 * Copyright © 2013-2016 The Hyve B.V.
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

package org.transmartproject.db.dataquery.highdim.snp_lz

import com.google.common.base.Charsets
import com.google.common.collect.ImmutableTable
import com.google.common.collect.Table
import grails.util.Holders
import org.hibernate.LobHelper
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.I2b2;
import org.transmartproject.db.ontology.TableAccess;

import static org.transmartproject.db.ontology.ConceptTestData.createI2b2Concept
import static org.transmartproject.db.ontology.ConceptTestData.createTableAccess
import static org.transmartproject.db.ontology.ConceptTestData.createConceptDimensions

import java.util.zip.GZIPOutputStream

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class SnpLzTestData {
    public static final String TRIAL_NAME = 'CARDS'
    public static final String CONCEPT_PATH = "\\\\i2b2 main\\foo\\${TRIAL_NAME}\\concept code #1"
    public static final String PLATFORM = 'Perlegen_600k'
    public List<String> conceptCodes = ['concept code #1', 'concept code #2']

    List<DeSubjectSampleMapping> assays

    SnpLzTestData() {
        assays = HighDimTestData.createTestAssays(
                patients, -400, platforms[0], TRIAL_NAME, concepts[1].conceptCode, platforms[0].title)
        assays += HighDimTestData.createTestAssays(
                patients, -200, platforms[1], TRIAL_NAME, concepts[2].conceptCode, platforms[1].title)
    }

    List<TableAccess> i2b2TopConcepts = [
            createTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                    tableCode: 'i2b2 main', tableName: 'i2b2'),
    ]

    List<I2b2> i2b2Concepts = [
            createI2b2Concept(code: 1, level: 1, fullName: "\\foo\\${TRIAL_NAME}\\", name: TRIAL_NAME),
            createI2b2Concept(code: 2, level: 2, fullName: "\\foo\\${TRIAL_NAME}\\${conceptCodes[0]}\\", name: conceptCodes[0]),
            createI2b2Concept(code: 3, level: 2, fullName: "\\foo\\${TRIAL_NAME}\\${conceptCodes[1]}\\", name: conceptCodes[1]),
    ]

    List<ConceptDimension> concepts = createConceptDimensions(i2b2Concepts)

    List<DeGplInfo> platforms = {
        def createPlatform = { title,
                               organism,
                               markerType ->
            def res = new DeGplInfo(
                    title: title,
                    organism: organism,
                    markerType: markerType)
            res.id = title
            res
        }
        [
                createPlatform('Perlegen_600k', 'Homo Sapiens', 'SNP'),
                createPlatform('CARDSSNP', 'Homo Sapiens', 'SNP'),
        ]
    }()

    List<CoreBioAssayPlatform> bioAssayPlatforms = {
        def id = -800
        platforms.collect { platform ->
            def p = new CoreBioAssayPlatform(
                    accession:  platform.id,
                    organism:   platform.organism,
                    type:       platform.markerType
            )
            p.id = --id
            p
        }
    }()

    List<BioAssayGenoPlatformProbe> bioAssayGenoPlatformProbes = {
        def id = -1200
        bioAssayPlatforms.collect { bioAssayPlatform ->
            def bagpp = new BioAssayGenoPlatformProbe(
                    bioAssayPlatform: bioAssayPlatform)
            bagpp.id = --id
            bagpp
        }
    }()

    List<PatientDimension> patients =
            HighDimTestData.createTestPatients(3, -300, TRIAL_NAME)

    List<GenotypeProbeAnnotation> annotations = {
        def createAnnotation = { id,
                                 snpName,
                                 geneInfo,
                                 chromosome,
                                 pos,
                                 ref,
                                 alt ->
            def res = new GenotypeProbeAnnotation(
                    snpName: snpName,
                    geneInfo: geneInfo,
                    chromosome: chromosome,
                    pos: pos,
                    ref: ref,
                    alt: alt,
                    genomeBuild: 'GRCh37',
            )
            res.id = id
            res
        }

        [
                createAnnotation(-111197026, 'rs28616230',  'ND1:4535', '1', 4171, 'A', 'T'),
                createAnnotation(-111197028, 'rs1599988',   'ND1:4535', '1', 4216, 'C', 'G'),
                createAnnotation(-111197178, 'rs199476129', 'ND2:4536|COX1:4512', '1', 5920, 'C', 'T'),
        ]
    }()

    List<DeSnpGeneMap> snpGeneMaps = {
        def id = -600
        annotations.collectMany { ann ->
            List<DeSnpGeneMap> maps = []
            for (String gene: ann.geneInfo.tokenize('|')) {
                def parts = gene.tokenize(':')
                def geneName = parts[0]
                def geneId = parts[1]
                def g = new DeSnpGeneMap(
                        snpName:        ann.snpName,
                        entrezGeneId:   geneId,
                        geneName:       geneName
                )
                g.id = --id
                maps += g
            }
            maps
        }
    }()

    @Lazy
    List<SnpSubjectSortedDef> sortedSubjects = {
        def id = -500
        def pos = 1 /* positions are 1-based */
        assays.collect { assay ->
            def bioAssayPlatform = bioAssayPlatforms.find {
                it.accession == assay.platform.id
            }
            def s = new SnpSubjectSortedDef(
                    trialName: TRIAL_NAME,
                    patientPosition: pos++,
                    patient: assay.patient,
                    bioAssayPlatform: bioAssayPlatform,
                    subjectId: assay.sampleCode,
                    assayId: null
            )
            s.id = --id
            s
        }
    }()

    @Lazy
    def orderedSampleCodes = assays.sort { a ->
        sortedSubjects.find { it.subjectId == a.sampleCode }.patientPosition
    }*.sampleCode

    @Lazy
    def orderedSampleCodesByPlatform = platforms.collectEntries { platform ->
        [(platform.id):
                 assays.findAll { it.platform.id == platform.id }.sort { a ->
                     sortedSubjects.find { it.subjectId == a.sampleCode }.patientPosition
                 }*.sampleCode
        ]
    }

    @Lazy
    def orderedAssays = assays.sort { it.id }

    @Lazy
    def orderedAssaysByPlatform = platforms.collectEntries { platform ->
        [(platform.id):
                 assays.findAll { it.platform.id == platform.id }.sort { it.id }
        ]
    }

    @Lazy
    Table<String /* sample code*/, String /* rs id */, String /* triplet */> sampleGps = {
        def tb = ImmutableTable.builder()

        tb.put assays[0].sampleCode, annotations[0].snpName, '0 0 1'
        tb.put assays[0].sampleCode, annotations[1].snpName, '0 1 0'
        tb.put assays[0].sampleCode, annotations[2].snpName, '0 0 0'

        tb.put assays[1].sampleCode, annotations[0].snpName, '1 0 0'
        tb.put assays[1].sampleCode, annotations[1].snpName, '0 0 0'
        tb.put assays[1].sampleCode, annotations[2].snpName, '1 0 0'

        tb.put assays[2].sampleCode, annotations[0].snpName, '0 0 1'
        tb.put assays[2].sampleCode, annotations[1].snpName, '0 0 1'
        tb.put assays[2].sampleCode, annotations[2].snpName, '0 1 0'

        tb.put assays[3].sampleCode, annotations[0].snpName, '0 0 1'
        tb.put assays[3].sampleCode, annotations[1].snpName, '0 1 0'
        tb.put assays[3].sampleCode, annotations[2].snpName, '0 0 0'

        tb.put assays[4].sampleCode, annotations[0].snpName, '1 0 0'
        tb.put assays[4].sampleCode, annotations[1].snpName, '0 0 0'
        tb.put assays[4].sampleCode, annotations[2].snpName, '1 0 0'

        tb.put assays[5].sampleCode, annotations[0].snpName, '0 0 1'
        tb.put assays[5].sampleCode, annotations[1].snpName, '0 0 1'
        tb.put assays[5].sampleCode, annotations[2].snpName, '0 1 0'

        tb.build()
    }()

    @Lazy
    Table<String /* sample code*/, String /* rs id */, String /* alleles */> sampleGts = {
        def tb = ImmutableTable.builder()

        tb.put assays[0].sampleCode, annotations[0].snpName, 'A A'
        tb.put assays[0].sampleCode, annotations[1].snpName, 'A T'
        tb.put assays[0].sampleCode, annotations[2].snpName, 'N N'

        tb.put assays[1].sampleCode, annotations[0].snpName, 'T T'
        tb.put assays[1].sampleCode, annotations[1].snpName, 'N N'
        tb.put assays[1].sampleCode, annotations[2].snpName, 'T T'

        tb.put assays[2].sampleCode, annotations[0].snpName, 'A A'
        tb.put assays[2].sampleCode, annotations[1].snpName, 'A A'
        tb.put assays[2].sampleCode, annotations[2].snpName, 'A T'

        tb.put assays[3].sampleCode, annotations[0].snpName, 'A A'
        tb.put assays[3].sampleCode, annotations[1].snpName, 'A T'
        tb.put assays[3].sampleCode, annotations[2].snpName, 'N N'

        tb.put assays[4].sampleCode, annotations[0].snpName, 'T T'
        tb.put assays[4].sampleCode, annotations[1].snpName, 'N N'
        tb.put assays[4].sampleCode, annotations[2].snpName, 'T T'

        tb.put assays[5].sampleCode, annotations[0].snpName, 'A A'
        tb.put assays[5].sampleCode, annotations[1].snpName, 'A A'
        tb.put assays[5].sampleCode, annotations[2].snpName, 'A T'

        tb.build()
    }()

    @Lazy
    Table<String /* sample code*/, String /* rs id */, String /* dosage */> sampleDoses = {
        def tb = ImmutableTable.builder()

        tb.put assays[0].sampleCode, annotations[0].snpName, '0'
        tb.put assays[0].sampleCode, annotations[1].snpName, '1'
        tb.put assays[0].sampleCode, annotations[2].snpName, '0'

        tb.put assays[1].sampleCode, annotations[0].snpName, '2'
        tb.put assays[1].sampleCode, annotations[1].snpName, '0'
        tb.put assays[1].sampleCode, annotations[2].snpName, '2'

        tb.put assays[2].sampleCode, annotations[0].snpName, '0'
        tb.put assays[2].sampleCode, annotations[1].snpName, '0'
        tb.put assays[2].sampleCode, annotations[2].snpName, '1'

        tb.put assays[3].sampleCode, annotations[0].snpName, '0'
        tb.put assays[3].sampleCode, annotations[1].snpName, '1'
        tb.put assays[3].sampleCode, annotations[2].snpName, '0'

        tb.put assays[4].sampleCode, annotations[0].snpName, '2'
        tb.put assays[4].sampleCode, annotations[1].snpName, '0'
        tb.put assays[4].sampleCode, annotations[2].snpName, '2'

        tb.put assays[5].sampleCode, annotations[0].snpName, '0'
        tb.put assays[5].sampleCode, annotations[1].snpName, '0'
        tb.put assays[5].sampleCode, annotations[2].snpName, '1'

        tb.build()
    }()


    def lobotomize(LobHelper lobHelper, String s) {
        def os = new ByteArrayOutputStream()
        def gzipOs = new GZIPOutputStream(os)
        gzipOs.write(s.getBytes(Charsets.UTF_8))
        gzipOs.close()
        lobHelper.createBlob(os.toByteArray())
    }

    @Lazy
    List<SnpDataByProbeCoreDb> data = {
        def session = Holders.applicationContext.sessionFactory.currentSession
        def lobHelper = session.lobHelper

        def createDataEntry = { id,
                                GenotypeProbeAnnotation annotation,
                                BioAssayGenoPlatformProbe platformProbe,
                                /* these are by order of patient */
                                List<String> gpss /* ['0 0 1', '0 1 0', ...] */,
                                gtss /* ['T T', 'T A', ...] */,
                                doses /* [0, 1, ...] */ ->
            def a1 = annotation.alt /* though A1 is not always the minor allele */
            def a2 = annotation.ref /* though A2 is not always the major allele */

            assert !(null in gpss) && !(null in gtss) && !(null in doses)

            // Choose value of minorAllele based on the allele count, to vary its
            // value: to generate test cases for both 'A1' and 'A2'.
            long a1Count = 0
            long a2Count = 0
            gtss.each {
                a1Count += it.count( a1 )
                a2Count += it.count( a2 )
            }
            def minorAllele = (a1Count <= a2Count) ? 'A1' : 'A2'

            def r = new SnpDataByProbeCoreDb(
                    trialName: TRIAL_NAME,
                    a1: a1,
                    a2: a2,
                    gtProbabilityThreshold: 1.0,
                    imputeQuality: 0.1 * id,
                    maf: 0.218605627887442,
                    minorAllele: minorAllele,
                    countA1A1: gtss.count { "$a1 $a1" },
                    countA1A2: gtss.count { "$a1 $a2" } + gtss.count { "$a2 $a1" },
                    countA2A2: gtss.count { "$a2 $a2" },
                    countNocall: gtss.count { 'N N' },
                    genotypeProbeAnnotation: annotation,
                    bioAssayGenoPlatform: platformProbe,
                    gpsByProbeBlob: lobotomize(lobHelper, gpss.join(' ')),
                    gtsByProbeBlob: lobotomize(lobHelper, gtss.join(' ')),
                    doseByProbeBlob: lobotomize(lobHelper, doses.join(' ')),
            )

            r.id = id
            r
        }

        def i = -700
        annotations.collectMany { ann ->
            bioAssayGenoPlatformProbes.collect { BioAssayGenoPlatformProbe platformProbe ->
                def sampleCodes = orderedSampleCodesByPlatform[platformProbe.bioAssayPlatform.accession]
                createDataEntry(
                        --i,
                        ann,
                        platformProbe,
                        sampleCodes.collect { sc -> sampleGps.get(sc, ann.snpName) },
                        sampleCodes.collect { sc -> sampleGts.get(sc, ann.snpName) },
                        sampleCodes.collect { sc -> sampleDoses.get(sc, ann.snpName) })
            }
        }
    }()

    void saveAll() {
        save i2b2TopConcepts
        save i2b2Concepts
        save concepts
        save platforms
        save bioAssayPlatforms
        save bioAssayGenoPlatformProbes
        save patients
        save assays
        save annotations
        save snpGeneMaps
        save sortedSubjects
        save data
    }
}
