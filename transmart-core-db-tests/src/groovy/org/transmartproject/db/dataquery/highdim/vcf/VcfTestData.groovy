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

package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.PatientDimension

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

/**
 * Created by j.hudecek on 13-3-14.
 */
class VcfTestData  {

    public static final String TRIAL_NAME = 'VCF_SAMP_TRIAL'

    DeGplInfo platform
    DeGplInfo otherPlatform
    DeVariantDatasetCoreDb dataset
    List<PatientDimension> patients
    List<DeSubjectSampleMapping> assays
    List<DeVariantSubjectSummaryCoreDb> summariesData
    List<DeVariantSubjectDetailCoreDb> detailsData
    List<DeVariantSubjectIdxCoreDb> indexData
    
    public VcfTestData(String conceptCode = 'bogus') {
        // Create VCF platform and assays
        platform = new DeGplInfo(
                    title: 'Test VCF',
                    organism: 'Homo Sapiens',
                    markerType: 'VCF')
        platform.id = 'BOGUSGPLVCF'
        dataset = new DeVariantDatasetCoreDb(genome:'human')
        dataset.id = 'BOGUSDTST'
        patients = HighDimTestData.createTestPatients(3, -800, TRIAL_NAME)
        assays = HighDimTestData.createTestAssays(patients, -1400, platform, TRIAL_NAME, conceptCode)
        
        // Create VCF data
        detailsData = []
        detailsData += createDetail(1, 'C', 'A', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')
        detailsData += createDetail(2, 'GCCCCC', 'GCCCC', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')
        detailsData += createDetail(3, 'A', 'C,T', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')

        indexData = []
        assays.eachWithIndex { assay, idx ->
            indexData << new DeVariantSubjectIdxCoreDb(
                dataset: dataset,
                subjectId: assay.sampleCode,
                position: idx + 1
            )
        }
        
        summariesData = []
        detailsData.each { detail ->
            // Create VCF summary entries with the following variants:
            // 1/0, 0/1 and 1/1
            int mut = 0
            assays.eachWithIndex { assay, idx ->
                mut++
                summariesData += createSummary detail, mut & 1, (mut & 2) >> 1,  assay, indexData[idx]
            }
            if (detail.alt.contains(','))
                summariesData.last().allele1=2
        }
        
        // Add also another platform and assays for those patients
        // to test whether the VCF module only returns VCF assays
        otherPlatform = new DeGplInfo(
            title: 'Other platform',
            organism: 'Homo Sapiens',
            markerType: 'mrna')
        otherPlatform.id = 'BOGUSGPLMRNA'
        
        assays += HighDimTestData.createTestAssays(patients, -1800, otherPlatform, "OTHER_TRIAL")
    }

    def createDetail = {
        int position,
        String reference,
        String alternative,
        String info
            ->
            new DeVariantSubjectDetailCoreDb(
                    chr: 1,
                    pos: position,
                    rsId: '.',
                    ref: reference,
                    alt: alternative,
                    quality: position, //nonsensical value
                    filter: '.',
                    info:  info,
                    format: 'GT',
                    dataset: dataset,
                    variant: "" + position + "/" + position + "\t" +
                            (position + 1) + "/" + (position + 1) + "\t" +
                            (position * 2) + "/" + (position * 2)
            )
    }

    def createSummary = {
        DeVariantSubjectDetailCoreDb detail,
        int allele1,
        int allele2,
        DeSubjectSampleMapping assay,
        DeVariantSubjectIdxCoreDb subjectIndex
            ->
            new DeVariantSubjectSummaryCoreDb(
                    dataset: dataset,
                    chr: 1,
                    pos: detail.pos,
                    rsId: '.',
                    subjectId: subjectIndex.subjectId,
                    variant: ((allele1 == 0) ? detail.ref : detail.alt) + '/' +
                            ((allele2 == 0) ? detail.ref : detail.alt),
                    variantFormat: ((allele1 == 0) ? 'R':'V') + '/' +
                            ((allele2 == 0) ? 'R':'V'),
                    variantType: detail.ref.length() > 1 ? 'DIV' : 'SNV',
                    reference: true,
                    allele1: allele1,
                    allele2: allele2,
                    assay: assay,
                    jDetail: detail,
                    subjectIndex: subjectIndex
            )
    }




    void saveAll() {
        assertThat platform.save(), is(notNullValue(DeGplInfo))
        assertThat otherPlatform.save(), is(notNullValue(DeGplInfo))
        save([dataset])
        save patients
        save assays
        save detailsData
        save indexData
        save summariesData
    }
}
