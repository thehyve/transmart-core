package com.recomdata.grails.plugin.gwas

import grails.plugin.cache.Cacheable
// from transmart-extensions
import org.transmart.biomart.Experiment


@Cacheable('com.recomdata.grails.plugin.gwas.ExperimentService')
class ExperimentService {

    public String getExperimentAccession(Long experimentId) {
        if (!experimentId) {
            return null
        }
        def experiment = Experiment.get(experimentId)

        if (experiment == null) {
            return null
        }
        experiment.accession
    }

}

