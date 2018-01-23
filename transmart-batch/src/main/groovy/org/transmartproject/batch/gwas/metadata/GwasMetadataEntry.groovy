package org.transmartproject.batch.gwas.metadata

import groovy.transform.ToString

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size
import javax.validation.constraints.Digits

/**
 * Represents a line in the GWAS metadata file.
 */
@ToString
class GwasMetadataEntry {
    public final static String GWAS_DATA_TYPE = 'GWAS'

    @NotNull
    @Size(min = 1, max = 50)
    String study // STUDY ID field in Kettle

    @NotNull
    @Pattern(regexp = 'GWAS', message = '{gwasDataTypes}')
    String dataType

    @NotNull
    @Size(min = 1, max = 500)
    String analysisName

    @NotNull
    @Size(min = 1, max = 500)
    String analysisNameArchived // default to analysisName

    @Size(max = 2048)
    String description

    /**
     * PHENOTYPE_NAMES field in Kettle
     * Only one not inserted in lz_src_analysis_metadata.
     * Doesn't seem to be used at all
     */
    String phenotypeNames

    /**
     * PHENOTYPES field in Kettle,
     * PHENOTYPE_IDS in lz_src_analysis_metadata
     */
    @Size(max = 250)
    String phenotypeSourceAndCodes //

    @Size(max = 500)
    String population

    @Size(max = 500)
    String tissue

    @Pattern(regexp = '1[8-9]', message = '{supportedGenomeVersions}')
    String genomeVersion

    @Size(max = 500)
    String genotypePlatformName // genotype_platform_ids in lz_src_analysis_metadata

    @Size(max = 500)
    String expressionPlatformName // expression_platform_in lz_src_analysis_metadata

    @Size(max = 500)
    String statisticalTest

    @Size(max = 500)
    String modelName

    @Size(max = 500)
    String modelDesc

    @Size(max = 500)
    String researchUnit

    @Size(max = 500)
    String sampleSize

    @Size(max = 500)
    String cellType

    @Digits(integer = 1, fraction = 10)
    BigDecimal pvalueCutoff

    @Size(max = 500)
    String inputFile // INPUT_FILENAME in kettle

    String getAnalysisNameArchived() {
        analysisNameArchived ?: analysisName
    }
}
