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

import org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType
import org.transmartproject.core.dataquery.highdim.vcf.VcfCohortInfo

class VcfCohortStatistics implements VcfCohortInfo {
    protected VcfDataRow dataRow
    
    // Allele information for the alleles in this cohort
    List<String> alleles = []
    List<Integer> alleleCount = []
    int totalAlleleCount = 0
    int numberOfSamplesWithData = 0
    List<GenomicVariantType> genomicVariantTypes = []

    // Cohort level properties
    String majorAllele = "."
    String minorAllele = "."
    Double minorAlleleFrequency = 0.0
    
    VcfCohortStatistics( VcfDataRow dataRow ) {
        this.dataRow = dataRow
        
        computeCohortStatistics()
    }
    
    @Override
    List<Double> getAlleleFrequency() {
        alleleCount.collect {  it / totalAlleleCount }
    }

    @Override
    String getReferenceAllele() {
        dataRow.referenceAllele
    }

    @Override
    List<String> getAlternativeAlleles() {
        alleles - referenceAllele
    }

    /**
     * Computes cohort level statistics
     */
    protected computeCohortStatistics() {
        Map<String,Integer> numAlleles = countAlleles()

        if( !numAlleles )
            return

        // Store generic allele distribution
        //TODO the order of keys and values is predictable and corresponds implicitly in this case (LinkedHashMap).
        alleles = new ArrayList<String>(numAlleles.keySet())
        alleleCount = new ArrayList<Integer>(numAlleles.values())
        totalAlleleCount = alleleCount.sum()

        // Find the most frequent and second most frequent alleles
        majorAllele = numAlleles.max { it.value }?.key
        minorAllele = numAlleles.findAll { it.key != majorAllele }.max { it.value }?.key ?: "."
        if( minorAllele != "." )
            minorAlleleFrequency = numAlleles.getAt( minorAllele ) / totalAlleleCount
            
        // Determine genomic variant types, with the major allele as a reference
        genomicVariantTypes = getGenomicVariantTypes( majorAllele, alleles)
    }
    
    // Allele distribution for the current cohort
    Map<String,Integer> countAlleles( ) {
        List alleleNames = [] + dataRow.referenceAllele + dataRow.alternativeAlleles
        def alleleDistribution = [:].withDefault { 0 }
        
        numberOfSamplesWithData = 0
        for (sampleData in dataRow.data) {
            if ( !sampleData )
                continue;
            
            boolean sampleHasData = false
            if (sampleData.allele1 != null && sampleData.allele1 != ".") {
                def allele1 = alleleNames[sampleData.allele1]
                alleleDistribution[allele1]++
                sampleHasData = true
            }
            
            if (sampleData.allele2 != null && sampleData.allele2 != ".") {
                def allele2 = alleleNames[sampleData.allele2]
                alleleDistribution[allele2]++
                sampleHasData = true
            }
            
            if (sampleHasData) {
                numberOfSamplesWithData++
            }
        }
        alleleDistribution
    }
    
    List<GenomicVariantType> getGenomicVariantTypes(Collection<String> alleleCollection) {
        getGenomicVariantTypes(majorAllele, altCollection)
    }

    List<GenomicVariantType> getGenomicVariantTypes(String ref, Collection<String> alleleCollection) {
        alleleCollection.collect{
            ref == it ? null : GenomicVariantType.getGenomicVariantType(ref, it) 
        }
    }

    private List<Double> parseNumbersList(String numbersString) {
        parseCsvString(numbersString) {
            it.isNumber() ? Double.valueOf(it) : null
        }
    }

    private List parseCsvString(String string, Closure typeConverterClosure = { it }) {
        if (!string) {
            return []
        }

        string.split(/\s*,\s*/).collect {
            typeConverterClosure(it)
        }
    }
}
