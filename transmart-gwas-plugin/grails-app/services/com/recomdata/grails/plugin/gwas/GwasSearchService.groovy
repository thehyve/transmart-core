package com.recomdata.grails.plugin.gwas

import org.apache.log4j.Logger
import org.transmart.biomart.BioAssayAnalysisDataIdx

class GwasSearchService {
    static Logger log = Logger.getLogger(GwasSearchService.class)

    def getGwasData(analysisId) {
        getGwasData(analysisId, null)
    }

    def getGwasData(analysisId, ranges) {
        def queryParams = [parAnalysisId: analysisId]
        StringBuilder qb = new StringBuilder("""
					SELECT	gwas.rsId,
							gwas.pValue,
							gwas.logPValue,
							gwas.ext_data
					FROM	BioAssayAnalysisGwas gwas
					WHERE	gwas.analysis.id = :parAnalysisId""")

        if (ranges) {
            qb.append(" AND gwas.rsId IN (:parSearchProbes)");
        }
        def results = BioAssayAnalysisGwas.executeQuery(qb.toString(), queryParams, [max: 100])
        return results
    }

    def getGwasIndexData() {
        def results = BioAssayAnalysisDataIdx.findAllByExt_type("GWAS", [sort: "display_idx", order: "asc"])

        return results
    }

    def getEqtlIndexData() {
        def results = BioAssayAnalysisDataIdx.findAllByExt_type("EQTL", [sort: "display_idx", order: "asc"])

        return results
    }

    def getEqtlData(analysisId) {
        getEqtlData(analysisId, null)
    }

    def getEqtlData(analysisId, searchProbes) {
        def results = BioAssayAnalysisEqtl.executeQuery("""
			SELECT	eqtl.rsId,
					eqtl.pValue,
					eqtl.logPValue,
					eqtl.ext_data
			FROM	BioAssayAnalysisEqtl eqtl
			WHERE	eqtl.analysis.id = :parAnalaysisId
			""", [parAnalaysisId: analysisId], [max: 100])

        return results
    }

}