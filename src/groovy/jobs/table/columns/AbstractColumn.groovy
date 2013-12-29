package jobs.table.columns

import groovy.transform.CompileStatic
import jobs.table.BackingMap
import jobs.table.Column
import jobs.table.MissingValueAction

@CompileStatic
abstract class AbstractColumn implements Column {

    String columnHeader

    MissingValueAction missingValueAction =
            new MissingValueAction.ConstantReplacementMissingValueAction(replacement: '')

    @Override
    void onDataSourceDepleted(String dataSourceName, Iterable dataSource, BackingMap backingMap) {
        /* override to do something here */
    }

    @Override
    void beforeDataSourceIteration(String dataSourceName, Iterable dataSource) {
        /* override to do something here */
    }
}
