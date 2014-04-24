
package org.transmart.biomart

import com.recomdata.util.IExcelProfile

class BioAssayAnalysis implements IExcelProfile {
    String name
    String shortDescription
    String longDescription
    Date createDate
    String qaCriteria
    String analystId
    Long id
    Double foldChangeCutoff
    Double pValueCutoff
    Double rValueCutoff
    BioAssayAnalysisPlatform analysisPlatform
    String assayDataType
    String analysisMethodCode
    String type
    Long dataCount
    Long teaDataCount
    static hasMany = [datasets: BioAssayDataset, files: ContentReference]
    static belongsTo = [ContentReference]

    static mapping = {
        table 'BIO_ASSAY_ANALYSIS'
        version false
        cache usage: 'read-only'
        id generator: 'sequence', params: [sequence: 'SEQ_BIO_DATA_ID']
        columns {
            name column: 'ANALYSIS_NAME'
            shortDescription column: 'SHORT_DESCRIPTION'
            longDescription column: 'LONG_DESCRIPTION'
            createDate column: 'ANALYSIS_CREATE_DATE'
            qaCriteria column: 'QA_CRITERIA'
            analystId column: 'ANALYST_ID'
            id column: 'BIO_ASSAY_ANALYSIS_ID'
            foldChangeCutoff column: 'FOLD_CHANGE_CUTOFF'
            pValueCutoff column: 'PVALUE_CUTOFF'
            rValueCutoff column: 'RVALUE_CUTOFF'
            analysisPlatform column: 'BIO_ASY_ANALYSIS_PLTFM_ID'
            type column: 'ANALYSIS_TYPE'
            dataCount column: 'DATA_COUNT'
            teaDataCount column: 'TEA_DATA_COUNT'
            assayDataType column: 'BIO_ASSAY_DATA_TYPE'
            analysisMethodCode column: 'ANALYSIS_METHOD_CD'
            datasets joinTable: [name: 'BIO_ASY_ANALYSIS_DATASET', key: 'BIO_ASSAY_ANALYSIS_ID']
            files joinTable: [name: 'BIO_CONTENT_REFERENCE', key: 'BIO_DATA_ID', column: 'BIO_CONTENT_REFERENCE_ID'], cache: true


        }
    }

    static constraints = {
        name(nullable: true, maxSize: 1000)
        shortDescription(nullable: true, maxSize: 1020)
        longDescription(nullable: true, maxSize: 4000)
        createDate(nullable: true)
        qaCriteria(nullable: true, maxSize: 4000)
        analystId(nullable: true, maxSize: 1020)
        foldChangeCutoff(nullable: true)
        pValueCutoff(nullable: true)
        rValueCutoff(nullable: true)
        analysisPlatform(nullable: true)
        type(nullable: true, maxSize: 400)
    }

    /**
     * get top analysis data records for the indicated analysis
     */
    def static getTopAnalysisDataForAnalysis(Long analysisId, int topCount) {
        def query = "SELECT DISTINCT baad, baad_bm, ABS(baad.foldChangeRatio) FROM org.transmart.biomart.BioAssayAnalysisData baad JOIN baad.featureGroup.markers baad_bm  WHERE baad.analysis.id =:aid ORDER BY ABS(baad.foldChangeRatio) desc, baad.rValue, baad.rhoValue DESC";
        return BioAssayAnalysisData.executeQuery(query, [aid: analysisId], [max: topCount]);
    }

    /**
     * Get values to Export to Excel
     */
    public List getValues() {
        return [shortDescription, longDescription, pValueCutoff, foldChangeCutoff, qaCriteria, analysisPlatform == null ? "" : analysisPlatform.platformName, analysisMethodCode, assayDataType]
    }
}
