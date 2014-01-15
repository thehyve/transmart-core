package jobs

import jobs.table.columns.PrimaryKeyColumn
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection

@Component
@Scope('job')
class TableWithFisher extends CategoricalOrBinnedJob {

    @Override
    void afterPropertiesSet() throws Exception {
        primaryKeyColumnConfigurator.column =
                new PrimaryKeyColumn(header: 'PATIENT_NUM')

        configureConfigurator independentVariableConfigurator, 'indep', 'independent', 'X'
        configureConfigurator dependentVariableConfigurator,   'dep',   'dependent',   'Y'
    }

    @Override
    protected List<String> getRStatements() {
        [ '''source('$pluginDirectory/TableWithFisher/FisherTableLoader.R')''',
                '''FisherTable.loader(input.filename='outputfile')''' ]
    }

    @Override
    protected getForwardPath() {
        """/tableWithFisher/fisherTableOut?jobName=$name"""
    }
}
