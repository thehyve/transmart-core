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
        
        assertThat cohortStatistics.alleleDistribution.size(), equalTo(0)
        assertThat cohortStatistics.maf, closeTo(0.0 as Double, 0.001 as Double)
        assertThat cohortStatistics.mafAllele, equalTo(".")
        assertThat cohortStatistics.alternativeAlleles, hasSize(0)
        assertThat cohortStatistics.genomicVariantTypes, hasSize(0)
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
            alternatives: "A,T,CG",
            
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
        
        println cohortStatistics.alleleDistribution
        
        assertThat cohortStatistics.alleleDistribution.size(), equalTo(4)
        assertThat cohortStatistics.maf, closeTo( 0.4 as Double, 0.001 as Double )
        assertThat cohortStatistics.mafAllele, equalTo("A")
        assertThat cohortStatistics.alternativeAlleles, hasSize(3)
        assertThat cohortStatistics.alternativeAlleles, hasItem( equalTo( "A" ) )
        assertThat cohortStatistics.alternativeAlleles, hasItem( equalTo( "T" ) )
        assertThat cohortStatistics.alternativeAlleles, hasItem( equalTo( "CG" ) )
        
        assertThat cohortStatistics.genomicVariantTypes, hasSize(3)
        assertThat cohortStatistics.genomicVariantTypes, hasItem( equalTo( GenomicVariantType.INS ) )
        assertThat cohortStatistics.genomicVariantTypes, hasItem( equalTo( GenomicVariantType.SNP ) )
    }

}
