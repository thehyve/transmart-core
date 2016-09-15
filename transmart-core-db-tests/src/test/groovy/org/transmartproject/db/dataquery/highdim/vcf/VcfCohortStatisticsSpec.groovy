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
import spock.lang.Specification

import static org.hamcrest.Matchers.*

class VcfCohortStatisticsSpec extends Specification {


    public static final double ERROR = 0.001d

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
                info: "",
                format: "",
                variants: "",

                data: []
        )

        VcfCohortStatistics cohortStatistics = new VcfCohortStatistics(dataRow)

        expect:
        cohortStatistics.totalAlleleCount == 0
        cohortStatistics.alleles.empty
        cohortStatistics.alleleCount.empty
        cohortStatistics.alleleFrequency.empty
        cohortStatistics.genomicVariantTypes.empty

        cohortStatistics.majorAllele == "."
        cohortStatistics.minorAllele == "."
        cohortStatistics.minorAlleleFrequency - 0.0d < ERROR
        cohortStatistics.referenceAllele == "G"
        cohortStatistics.alternativeAlleles.empty
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
                info: "",
                format: "",
                variants: "",

                data: [
                        [allele1: 0, allele2: 0],
                        [allele1: 0, allele2: 0],
                        [allele1: 1, allele2: 0],
                        [allele1: 1, allele2: 1],
                        [allele1: 2, allele2: 1],
                        [allele1: 0, allele2: 2],
                ]
        )

        VcfCohortStatistics cohortStatistics = new VcfCohortStatistics(dataRow)

        expect:
        cohortStatistics.totalAlleleCount == 12
        cohortStatistics.alleles.size() == 3  // As 'CG' is not present anymore
        cohortStatistics.alleleCount.size() == 3
        cohortStatistics.alleleFrequency.size() == 3
        cohortStatistics.genomicVariantTypes.size() == 3

        cohortStatistics.majorAllele == "G"
        cohortStatistics.minorAllele == "A"
        cohortStatistics.minorAlleleFrequency - (4 / 12d) < ERROR

        cohortStatistics.genomicVariantTypes.size() == 3
        GenomicVariantType.INS in cohortStatistics.genomicVariantTypes
        cohortStatistics.genomicVariantTypes

        cohortStatistics.referenceAllele == "G"
        "A" in cohortStatistics.alternativeAlleles
        "TG" in cohortStatistics.alternativeAlleles
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
                info: "",
                format: "",
                variants: "",

                data: [
                        [allele1: 0, allele2: 0],
                        [allele1: 1, allele2: 0],
                        [allele1: 1, allele2: 1],
                        [allele1: 2, allele2: 1],
                        [allele1: 3, allele2: 2],
                ]
        )

        VcfCohortStatistics cohortStatistics = new VcfCohortStatistics(dataRow)

        expect:
        // We expect that the AT will be the reference in this cohort
        cohortStatistics.majorAllele == "AT"
        cohortStatistics.minorAllele == "G"
        cohortStatistics.minorAlleleFrequency - 0.3d < ERROR

        // We also expect that the genomic variants types are computed with the new major allele (AT) in mind
        cohortStatistics.genomicVariantTypes.size() == 4
        GenomicVariantType.DEL in cohortStatistics.genomicVariantTypes
        GenomicVariantType.DIV in cohortStatistics.genomicVariantTypes

        cohortStatistics.referenceAllele == "G"
        cohortStatistics.alternativeAlleles.size() == 3
        "AT" in cohortStatistics.alternativeAlleles
        "T" in cohortStatistics.alternativeAlleles
        "CG" in cohortStatistics.alternativeAlleles
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
                info: "",
                format: "",
                variants: "",

                data: [
                        [allele1: 0, allele2: 0],
                        [allele1: 1],
                        [allele1: ".", allele2: 1],
                        [allele1: 2, allele2: "."],
                        [allele1: null],
                        [allele1: null, allele2: 1],
                        [allele1: 1, allele2: null],
                ]
        )

        VcfCohortStatistics cohortStatistics = new VcfCohortStatistics(dataRow)

        expect:
        // We expect that the A will be the reference in this cohort
        cohortStatistics.majorAllele == "AT"
        cohortStatistics.minorAllele == "G"
        cohortStatistics.minorAlleleFrequency - (2 / 7d) < ERROR

        // We also expect that the genomic variants types are computed with the new major allele (AT) in mind
        cohortStatistics.alleleCount[cohortStatistics.alleles.indexOf("G")] == 2
        cohortStatistics.alleleCount[cohortStatistics.alleles.indexOf("AT")] == 4
        cohortStatistics.alleleCount[cohortStatistics.alleles.indexOf("T")] == 1

        cohortStatistics.referenceAllele == "G"
        cohortStatistics.alternativeAlleles.size() == 2
        "AT" in cohortStatistics.alternativeAlleles
        "T" in cohortStatistics.alternativeAlleles
    }

}
