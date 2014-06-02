package org.transmartproject.db.dataquery.highdim.vcf

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
class VcfDataRowTests {

    @Test
    void testAdditionalInfo() {
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
            info:  "DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268;NOVAL",
            format: "",
            variants: "",
        
            data: []
        )
        
        assertThat dataRow.infoFields, allOf(
            hasEntry(equalTo('DP'),equalTo('88')),
            hasEntry(equalTo('AF1'),equalTo('1')),
            hasEntry(equalTo('QD'),equalTo('2')),
            hasEntry(equalTo('DP4'),equalTo('0,0,80,0')),
            hasEntry(equalTo('MQ'),equalTo('60')),
            hasEntry(equalTo('FQ'),equalTo('-268')),
            
            hasEntry(equalTo( "NOVAL"), equalTo("Yes"))
        )
    }
    
    @Test
    void testQualityOfDepth() {
        VcfDataRow dataRow
        
        dataRow = new VcfDataRow(
            // Chromosome to define the position
            chromosome: 1,
            position: 500,
            rsId: "rs0001",
            
            // Reference and alternatives for this position
            referenceAllele: "G",
            alternatives: "A,T,CT",
            
            // Study level properties are irrelevant for the cohort statistics
            quality: 0.8,
            filter: "",
            info:  "DP=1.0;AB=1,201;TS=.;NOVAL",
            format: "",
            variants: "",
        
            data: []
        )
        
        assertThat dataRow.qualityOfDepth, closeTo( 0.8 as Double, 0.001 as Double )
        
        dataRow = new VcfDataRow(
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
            info:  "DP=1.0;AB=1,201;TS=.;NOVAL;QD=0.2",
            format: "",
            variants: "",
        
            data: []
        )
        
        assertThat dataRow.qualityOfDepth, closeTo( 0.2 as Double, 0.001 as Double )
    }

}
