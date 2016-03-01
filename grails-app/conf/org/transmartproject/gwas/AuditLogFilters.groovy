package org.transmartproject.gwas

// from transmart-core-api
import org.transmartproject.core.users.User
// from transmart-extensions
import org.transmart.biomart.BioAssayAnalysis

class AuditLogFilters {

    def auditLogService
	def experimentService
    User currentUserBean

	public String getAnalysisName(Long analysisId) {
		if (!analysisId) {
			return ""
		}
		BioAssayAnalysis analysis = BioAssayAnalysis.get(analysisId)
		if (!analysis) {
			return ""
		}
		analysis.name
	}

    def filters = {
		search(controller: 'GWAS', action: 'getFacetResults') {
			before = { model ->
				def fullUrl = "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
				auditLogService?.report("GWAS Active Filter", request,
						user: currentUserBean,
						query: params.q ?: '',
						facetQuery: params.fq ?: '',
				)
			}
		}
        other(controller: 'GWAS|gwas*|uploadData', action: '*', actionExclude:'getFacetResults|newSearch|index|getDynatree|getSearchCategories') {
            after = { model ->
                def fullUrl = "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
                def task = "Gwas (${controllerName}.${actionName})"
                switch (actionName) {
                    case "getAnalysisResults":
                        if (params.export) {
                            task = "Gwas CSV Export"
                        } else {
                            task = "Gwas Analysis Access"
                        }
                        break
                    case "getTrialAnalysis":
                        task = "Gwas Study Access"
                        break
                    case "getTableResults":
                        task = "Gwas Table View"
                        break
                }
                auditLogService?.report(task, request,
                        user: currentUserBean,
						experiment: experimentService.getExperimentAccession(params.getLong('trialNumber')) ?: '',
						analysis: getAnalysisName(params.getLong('analysisId')),
						export: params.export ?: '',
                )
            }
        }
    }

}
