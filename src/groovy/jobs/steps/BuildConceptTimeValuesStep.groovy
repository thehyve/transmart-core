package jobs.steps

import jobs.table.ConceptTimeValuesTable

/**
 * Created by carlos on 1/27/14.
 */
class BuildConceptTimeValuesStep implements Step {

    ConceptTimeValuesTable table

    @Override
    String getStatusName() {
        return 'Creating concept time values table'
    }

    @Override
    void execute() {
        table.compute()
    }

}
