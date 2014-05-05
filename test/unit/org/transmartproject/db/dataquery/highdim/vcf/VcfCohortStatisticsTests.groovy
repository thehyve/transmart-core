package org.transmartproject.db.dataquery.highdim.vcf

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.hibernate.ScrollableResults
import org.junit.Before;
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType;

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
    }
    
}
