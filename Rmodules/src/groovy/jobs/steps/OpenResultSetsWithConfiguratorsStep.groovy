package jobs.steps

import jobs.steps.helpers.ColumnConfigurator

class OpenResultSetsWithConfiguratorsStep implements Step {

    List<ColumnConfigurator> configurators

    final String statusName = 'Open Result Sets'

    @Override
    void execute() {
        configurators*.addColumn()
    }
}
