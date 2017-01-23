package org.transmartproject.batch.biodata

import javax.validation.constraints.Digits
import javax.validation.constraints.NotNull
import javax.validation.constraints.Past
import javax.validation.constraints.Size

/**
 * Pooling together the information about bio analyses stored in
 * BIO_ASSAY_ANALYSIS AND BIO_ASSAY_ANALYSIS_EXT.
 */
class BioAnalysis {

    @Size(max = 500)
    String analysisName

    @Size(max = 510)
    String shortDescription

    @Past
    Date analysisCreateDate

    @Size(max = 510)
    String analysisId

    @NotNull
    Long bioAssayAnalysisId

    @Size(max = 200)
    String analysisVersion

    @Digits(integer = 9, fraction = 2)
    BigDecimal foldChangeCutoff

    @Digits(integer = 9, fraction = 2)
    BigDecimal pvalueCutoff

    @Digits(integer = 9, fraction = 2)
    BigDecimal rvalueCutoff

    Long bioAssayAnalysisPlatformId

    Long bioSourceImportId

    @Size(max = 200)
    String analysisType

    @Size(max = 250)
    String analystName

    @Size(max = 50)
    String analysisMethodCode

    @Size(max = 20)
    String bioAssayDataType

    @Size(max = 100)
    String etlId

    @Size(max = 4000)
    String longDescription

    @Size(max = 4000)
    String qaCriteria

    Long dataCount

    Long teaDataCount

    @Digits(integer = 18, fraction = 5)
    BigDecimal lsmeanCutoff

    Long etlIdSource

    /* ext */
    Long bioAssayAnalysisExtId

    @Size(max = 500)
    String vendor

    @Size(max = 500)
    String vendorType

    @Size(max = 500)
    String genomeVersion

    @Size(max = 500)
    String tissue

    @Size(max = 500)
    String cellType

    @Size(max = 500)
    String population

    @Size(max = 500)
    String researchUnit

    @Size(max = 500)
    String sampleSize

    @Size(max = 100)
    String modelName

    @Size(max = 500)
    String modelDesc

    Long sensitiveFlag

    @Size(max = 500)
    String sensitiveDesc
}
