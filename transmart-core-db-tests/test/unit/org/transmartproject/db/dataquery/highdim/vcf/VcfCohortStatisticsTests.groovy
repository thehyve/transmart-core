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

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
class VcfCohortStatisticsTests {

    @Test
    void testEmptyDataRow() {
        VcfDataRow dataRow = new VcfDataRow(
            // Chromosome to define the position
            chromosome: 1,
            position: 500,
            rsId: "rs0001",
            
            // Reference and alternatives for this position
            referenceAllele: "G",
            alternatives: "A,T,CT",
            
            // Study level properties are irrelevant for the cohort statistics
            quality: 1.0,
            filter: "",
            info:  "",
            format: "",
            variants: "",
        
            data: []
        )
        
        VcfCohortStatistics cohortStatistics = new VcfCohortStatistics(dataRow)
        
        assertThat cohortStatistics.totalAlleleCount, equalTo(0)
        assertThat cohortStatistics.alleles.size(), equalTo(0)
        assertThat cohortStatistics.alleleCount.size(), equalTo(0)
        assertThat cohortStatistics.alleleFrequency.size(), equalTo(0)
        assertThat cohortStatistics.genomicVariantTypes, hasSize(0)
        
        assertThat cohortStatistics.majorAllele, equalTo(".")
        assertThat cohortStatistics.minorAllele, equalTo(".")
        assertThat cohortStatistics.minorAlleleFrequency, closeTo(0.0 as Double, 0.001 as Double)
        assertThat cohortStatistics.referenceAllele, equalTo("G")
        assertThat cohortStatistics.alternativeAlleles, empty()
    }

    @Test
    void testComputation() {
        VcfDataRow dataRow = new VcfDataRow(
            // Chromosome to define the position
            chromosome: 1,
            position: 500,
            rsId: "rs0001",
            
            // Reference and alternatives for this position
            referenceAllele: "G",
            alternatives: "A,TG,CG",
            
            // Study level properties are irrelevant for the cohort statistics
            quality: 1.0,
            filter: "",
            info:  "",
            format: "",
            variants: "",
        
            data: [
                [ allele1: 0, allele2: 0 ],
                [ allele1: 0, allele2: 0 ],
                [ allele1: 1, allele2: 0 ],
                [ allele1: 1, allele2: 1 ],
                [ allele1: 2, allele2: 1 ],
                [ allele1: 0, allele2: 2 ],
            ]
        )
            
        VcfCohortStatistics cohortStatistics = new VcfCohortStatistics(dataRow)
        
        assertThat cohortStatistics.totalAlleleCount, equalTo(12)
        assertThat cohortStatistics.alleles.size(), equalTo(3)  // As 'CG' is not present anymore
        assertThat cohortStatistics.alleleCount.size(), equalTo(3)
        assertThat cohortStatistics.alleleFrequency.size(), equalTo(3)
        assertThat cohortStatistics.genomicVariantTypes, hasSize(3)
        
        assertThat cohortStatistics.majorAllele, equalTo("G")
        assertThat cohortStatistics.minorAllele, equalTo("A")
        assertThat cohortStatistics.minorAlleleFrequency, closeTo(( 4 / 12 ) as Double, 0.001 as Double)
        
        assertThat cohortStatistics.genomicVariantTypes, hasSize(3)
        assertThat cohortStatistics.genomicVariantTypes, hasItem( equalTo( GenomicVariantType.INS ) )
        assertThat cohortStatistics.genomicVariantTypes, hasItem( equalTo( GenomicVariantType.SNP ) )

        assertThat cohortStatistics.referenceAllele, equalTo("G")
        assertThat cohortStatistics.alternativeAlleles, containsInAnyOrder("A", "TG")
    }

    @Test
    void testReferenceSwap() {
        VcfDataRow dataRow = new VcfDataRow(
            // Chromosome to define the position
            chromosome: 1,
            position: 500,
            rsId: "rs0001",
            
            // Reference and alternatives for this position
            referenceAllele: "G",
            alternatives: "AT,T,CG",
            
            // Study level properties are irrelevant for the cohort statistics
            quality: 1.0,
            filter: "",
            info:  "",
            format: "",
            variants: "",
        
            data: [
                [ allele1: 0, allele2: 0 ],
                [ allele1: 1, allele2: 0 ],
                [ allele1: 1, allele2: 1 ],
                [ allele1: 2, allele2: 1 ],
                [ allele1: 3, allele2: 2 ],
            ]
        )
            
        VcfCohortStatistics cohortStatistics = new VcfCohortStatistics(dataRow)
        
        // We expect that the AT will be the reference in this cohort
        assertThat cohortStatistics.majorAllele, equalTo("AT")
        assertThat cohortStatistics.minorAllele, equalTo("G")
        assertThat cohortStatistics.minorAlleleFrequency, closeTo(0.3 as Double, 0.001 as Double)
        
        // We also expect that the genomic variants types are computed with the new major allele (AT) in mind 
        assertThat cohortStatistics.genomicVariantTypes, hasSize(4)
        assertThat cohortStatistics.genomicVariantTypes, hasItem( equalTo( GenomicVariantType.DEL ) )
        assertThat cohortStatistics.genomicVariantTypes, hasItem( equalTo( GenomicVariantType.DIV ) )

        assertThat cohortStatistics.referenceAllele, equalTo("G")
        assertThat cohortStatistics.alternativeAlleles, containsInAnyOrder("AT", "T", "CG")
    }

    @Test
    void testCountingWithAllelesNotPresent() {
        VcfDataRow dataRow = new VcfDataRow(
            // Chromosome to define the position
            chromosome: 1,
            position: 500,
            rsId: "rs0001",
            
            // Reference and alternatives for this position
            referenceAllele: "G",
            alternatives: "AT,T,CG",
            
            // Study level properties are irrelevant for the cohort statistics
            quality: 1.0,
            filter: "",
            info:  "",
            format: "",
            variants: "",
        
            data: [
                [ allele1: 0, allele2: 0 ],
                [ allele1: 1 ],
                [ allele1: ".", allele2: 1 ],
                [ allele1: 2, allele2: "." ],
                [ allele1: null ],
                [ allele1: null, allele2: 1 ],
                [ allele1: 1, allele2: null ],
            ]
        )
            
        VcfCohortStatistics cohortStatistics = new VcfCohortStatistics(dataRow)
        
        // We expect that the A will be the reference in this cohort
        assertThat cohortStatistics.majorAllele, equalTo("AT")
        assertThat cohortStatistics.minorAllele, equalTo("G")
        assertThat cohortStatistics.minorAlleleFrequency, closeTo( (2 / 7) as Double, 0.001 as Double)
        
        // We also expect that the genomic variants types are computed with the new major allele (AT) in mind
        assertThat cohortStatistics.alleleCount[ cohortStatistics.alleles.indexOf( "G" ) ], equalTo(2)
        assertThat cohortStatistics.alleleCount[ cohortStatistics.alleles.indexOf( "AT" ) ], equalTo(4)
        assertThat cohortStatistics.alleleCount[ cohortStatistics.alleles.indexOf( "T" ) ], equalTo(1)

        assertThat cohortStatistics.referenceAllele, equalTo("G")
        assertThat cohortStatistics.alternativeAlleles, containsInAnyOrder("AT", "T")
    }
    
}
