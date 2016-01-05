package org.transmartproject.batch.gwas.biodata

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.biodata.BioExperimentDAO

/**
 * Inserts bio_experiment rows, if needed.
 */
@Component
class GwasBioExperimentWriter
        implements ItemWriter<String /* study id */> {

    @Autowired
    private BioExperimentDAO bioExperimentDAO

    @Override
    void write(List<? extends String> items) throws Exception {
        items.each {
            bioExperimentDAO.findOrCreateBioExperiment(
                    it, [bio_experiment_type: 'Experiment'])
        }
    }
}
