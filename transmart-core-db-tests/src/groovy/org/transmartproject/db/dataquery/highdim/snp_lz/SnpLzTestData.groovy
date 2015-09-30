/*
 * Copyright © 2013-2015 The Hyve B.V.
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
import org.transmartproject.db.i2b2data.PatientDimension

import java.util.zip.GZIPOutputStream

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class SnpLzTestData {
    public static final String TRIAL_NAME = 'CARDS'

    DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'Perlegen_600k',
                organism: 'Homo Sapiens',
                markerType: 'SNP')
        res.id = 'Perlegen_600k'
        res
    }()

    List<PatientDimension> patients =
        HighDimTestData.createTestPatients(3, -300, TRIAL_NAME)

    List<DeSubjectSampleMapping> assays =
        HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

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

    List<DeSnpInfo> snpInfos = {
        annotations.collect { ann ->
            def r = new DeSnpInfo(
                    name:       ann.snpName,
                    chromosome: ann.chromosome,
                    pos:        ann.pos,
            )

            r.id = ann.id * 10;
            r
        }
    }()

    List<DeRcSnpInfo> rcSnpInfos = {
        annotations.collect { ann ->
            def r = new DeRcSnpInfo(
                    chromosome: ann.chromosome,
                    pos:        ann.pos,
                    hgVersion:  '19',
                    geneName:   ann.geneInfo - ~/:.+/,
                    entrezId:   ann.geneInfo - ~/.+?:/ - ~/\|.+/,
            )

            r.id = ann.id * 10; // must be the same as for DeSnpInfo
            r
        }
    }()

    List<SnpSubjectSortedDef> sortedSubjects = {
        def id = -500
        def pos = 1 /* positions are 1-based */
        assays.collect { assay ->
            def s = new SnpSubjectSortedDef(
                    trialName: TRIAL_NAME,
                    patientPosition: pos++,
                    patient: assay.patient,
                    subjectId: assay.sampleCode,
            )
            s.id = --id
            s
        }
    }()

    def orderedSampleCodes = assays.sort { a ->
        sortedSubjects.find { it.subjectId == a.sampleCode }.patientPosition
    }*.sampleCode

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

        tb.build()
    }()

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

        tb.build()
    }()

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

        tb.build()
    }()


    def lobotomize(LobHelper lobHelper, String s) {
        def os = new ByteArrayOutputStream()
        def gzipOs = new GZIPOutputStream(os)
        gzipOs.write(s.getBytes(Charsets.UTF_8))
        gzipOs.close()
        lobHelper.createBlob(os.toByteArray())
    }

    List<SnpDataByProbeCoreDb> data = {
        def session = Holders.applicationContext.sessionFactory.currentSession
        def lobHelper = session.lobHelper

        def createDataEntry = { id,
                                GenotypeProbeAnnotation annotation,
                                /* these are by order of patient */
                                List<String> gpss /* ['0 0 1', '0 1 0', ...] */,
                                gtss /* ['T T', 'T A', ...] */,
                                doses /* [0, 1, ...] */ ->
            def a1 = annotation.alt /* though A1 is not always the minor allele */
            def a2 = annotation.ref /* though A2 is not always the major allele */

            assert !(null in gpss) && !(null in gtss) && !(null in doses)

            def snpInfo = snpInfos.find {
                it.pos == annotation.pos &&
                        it.chromosome == annotation.chromosome }
            assert snpInfo != null

            def r = new SnpDataByProbeCoreDb(
                    trialName: TRIAL_NAME,
                    a1: a1,
                    a2: a2,
                    gtProbabilityThreshold: 1.0,
                    imputeQuality: 0.1 * id,
                    maf: 0.218605627887442,
                    minorAllele: 'A1',
                    CA1A1: gtss.count { "$a1 $a1" },
                    CA1A2: gtss.count { "$a1 $a2" } + gtss.count { "$a2 $a1" },
                    CA2A2: gtss.count { "$a2 $a2" },
                    CNocall: gtss.count { 'N N' },
                    genotypeProbeAnnotation: annotation,
                    gpsByProbeBlob: lobotomize(lobHelper, gpss.join(' ')),
                    gtsByProbeBlob: lobotomize(lobHelper, gtss.join(' ')),
                    doseByProbeBlob: lobotomize(lobHelper, doses.join(' ')),
                    snpInfo: snpInfo,
            )

            r.id = id
            r
        }

        def i = -700
        annotations.collect { ann ->
            createDataEntry(
                    --i,
                    ann,
                    orderedSampleCodes.collect { sc -> sampleGps.get(sc, ann.snpName) },
                    orderedSampleCodes.collect { sc -> sampleGts.get(sc, ann.snpName) },
                    orderedSampleCodes.collect { sc -> sampleDoses.get(sc, ann.snpName) })
        }
    }()

    void saveAll() {
        save([platform])
        save patients
        save assays
        save annotations
        save snpInfos
        save rcSnpInfos
        save sortedSubjects
        save data
    }
}
