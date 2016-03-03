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
			return
		}
		BioAssayAnalysis analysis = BioAssayAnalysis.get(analysisId)
		if (!analysis) {
			return
		}
		analysis.name
	}

    public String getAnalysisNames(String analysisIds) {
        List<String> names = []
        List<String> ids = analysisIds?.split(",") ?: []
        for (String id: ids) {
            Long analysisId = id.toLong()
            String name = getAnalysisName(analysisId)
            if (name) {
                names += name
            }
        }
        names.join("|")
    }

    def filters = {
		search(controller: 'GWAS', action: 'getFacetResults') {
			before = { model ->
				auditLogService?.report("GWAS Active Filter", request,
						user: currentUserBean,
						query: params.q ?: '',
						facetQuery: params.fq ?: '',
				)
			}
		}
        other(controller: 'GWAS|gwas*|uploadData', action: '*', actionExclude:'getFacetResults|newSearch|index|getDynatree|getSearchCategories') {
            after = { model ->
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
                    case "webStartPlotter":
                        task = "Gwava"
                        break
                    case "exportAnalysis":
                        if (params.isLink == "true") {
                            task = "Gwas Files Export"
                        } else {
                            task = "Gwas Email Analysis"
                        }
                        break
                }
                String analysis = (params?.analysisIds) ?
                        getAnalysisNames(params.analysisIds) :
                        getAnalysisName(params.getLong('analysisId'))
                auditLogService?.report(task, request,
                        user: currentUserBean,
                        experiment: experimentService.getExperimentAccession(params.getLong('trialNumber')) ?: '',
                        analysis: analysis ?: '',
                        export: params.export ?: '',
                )
            }
        }
    }

}
