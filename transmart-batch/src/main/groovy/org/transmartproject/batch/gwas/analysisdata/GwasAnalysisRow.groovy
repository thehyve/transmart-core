package org.transmartproject.batch.gwas.analysisdata

import org.springframework.core.annotation.Order

/**
 * Represents an entry in the GWAS file.
 *
 * Fields by order in file. Annotations define order for the ext_data column.
 */
class GwasAnalysisRow {
    String rsId

    @Order(11)
    String allele1

    @Order(12)
    String allele2

    @Order(7)
    BigDecimal maf

    @Order(14)
    BigDecimal effect

    @Order(15)
    BigDecimal standardError

    BigDecimal pValue

    @Order(4)
    BigDecimal oddsRatio

    @Order(1)
    BigDecimal beta

    @Order(2)
    String directionOfEffect

    @Order(3)
    String hweControls

    @Order(5)
    String hweCases

    @Order(6)
    String hweTotalStudies

    @Order(8)
    String genotypeCountsCases

    @Order(9)
    String genoTypeCountsControls

    @Order(10)
    String totalNumberGenotyped

    @Order(13)
    String goodClustering

    @Order(17)
    String hetisq

    String hetpval

    @Order(16)
    String genotypeImputed

    String rsq

    @SuppressWarnings('PropertyName')
    @Order(18)
    String dose_B_0

    @SuppressWarnings('PropertyName')
    @Order(19)
    String dose_B_1
}
