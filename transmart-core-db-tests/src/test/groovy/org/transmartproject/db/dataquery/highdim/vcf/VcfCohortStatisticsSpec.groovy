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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType

import static org.hamcrest.Matchers.*

@Integration
@Rollback
@Slf4j
class VcfCohortStatisticsSpec extends Specification {

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
        
        expect: 
            cohortStatistics.totalAlleleCount equalTo(0)
            cohortStatistics.alleles.size() equalTo(0)
            cohortStatistics.alleleCount.size() equalTo(0)
            cohortStatistics.alleleFrequency.size() equalTo(0)
            cohortStatistics.genomicVariantTypes hasSize(0)
            
            cohortStatistics.majorAllele equalTo(".")
            cohortStatistics.minorAllele equalTo(".")
            cohortStatistics.minorAlleleFrequency closeTo(0.0 as Double, 0.001 as Double)
            cohortStatistics.referenceAllele equalTo("G")
            cohortStatistics.alternativeAlleles empty()
    }

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
        
        expect:
            cohortStatistics.totalAlleleCount equalTo(12)
            cohortStatistics.alleles.size() equalTo(3)  // As 'CG' is not present anymore
            cohortStatistics.alleleCount.size() equalTo(3)
            cohortStatistics.alleleFrequency.size() equalTo(3)
            cohortStatistics.genomicVariantTypes hasSize(3)
        
            cohortStatistics.majorAllele equalTo("G")
            cohortStatistics.minorAllele equalTo("A")
            cohortStatistics.minorAlleleFrequency closeTo(( 4 / 12 ) as Double, 0.001 as Double)

            cohortStatistics.genomicVariantTypes hasSize(3)
            cohortStatistics.genomicVariantTypes hasItem( equalTo( GenomicVariantType.INS ) )
            cohortStatistics.genomicVariantTypes hasItem( equalTo( GenomicVariantType.SNP ) )

            cohortStatistics.referenceAllele equalTo("G")
            cohortStatistics.alternativeAlleles containsInAnyOrder("A", "TG")
    }

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

        expect:
            // We expect that the AT will be the reference in this cohort
            cohortStatistics.majorAllele equalTo("AT")
            cohortStatistics.minorAllele equalTo("G")
            cohortStatistics.minorAlleleFrequency closeTo(0.3 as Double, 0.001 as Double)

            // We also expect that the genomic variants types are computed with the new major allele (AT) in mind
            cohortStatistics.genomicVariantTypes hasSize(4)
            cohortStatistics.genomicVariantTypes hasItem( equalTo( GenomicVariantType.DEL ) )
            cohortStatistics.genomicVariantTypes hasItem( equalTo( GenomicVariantType.DIV ) )

            cohortStatistics.referenceAllele equalTo("G")
            cohortStatistics.alternativeAlleles containsInAnyOrder("AT", "T", "CG")
    }

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

        expect:
            // We expect that the A will be the reference in this cohort
            cohortStatistics.majorAllele equalTo("AT")
            cohortStatistics.minorAllele equalTo("G")
            cohortStatistics.minorAlleleFrequency closeTo( (2 / 7) as Double, 0.001 as Double)

            // We also expect that the genomic variants types are computed with the new major allele (AT) in mind
            cohortStatistics.alleleCount[ cohortStatistics.alleles.indexOf( "G" ) ] equalTo(2)
            cohortStatistics.alleleCount[ cohortStatistics.alleles.indexOf( "AT" ) ] equalTo(4)
            cohortStatistics.alleleCount[ cohortStatistics.alleles.indexOf( "T" ) ] equalTo(1)

            cohortStatistics.referenceAllele equalTo("G")
            cohortStatistics.alternativeAlleles containsInAnyOrder("AT", "T")
    }
    
}
