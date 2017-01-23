package jobs

import jobs.steps.helpers.OptionalBinningColumnConfigurator
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('job')
class TableWithFisher extends CategoricalOrBinnedJob {

    @Autowired
    @Qualifier('general')
    OptionalBinningColumnConfigurator independentVariableConfigurator

    @Autowired
    @Qualifier('general')
    OptionalBinningColumnConfigurator dependentVariableConfigurator

    @Override
    void afterPropertiesSet() throws Exception {
        primaryKeyColumnConfigurator.column =
                new PrimaryKeyColumn(header: 'PATIENT_NUM')

        configureConfigurator independentVariableConfigurator,
                'indep', 'independent', 'X'
        configureConfigurator dependentVariableConfigurator,
                'dep',   'dependent',   'Y'
    }

    @Override
    protected List<String> getRStatements() {
        [ '''source('$pluginDirectory/TableWithFisher/FisherTableLoader.R')''',
                '''
                FisherTable.loader(
                input.filename               = '$inputFileName',
                aggregate.probes.independent = '$divIndependentVariableprobesAggregation' == 'true',
                aggregate.probes.dependent   = '$divDependentVariableprobesAggregation'   == 'true'
                )''' ]
    }

    @Override
    protected getForwardPath() {
        """/tableWithFisher/fisherTableOut?jobName=$name"""
    }
}
