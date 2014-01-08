package jobs.steps.helpers

import jobs.table.Column
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('prototype')
class SimpleAddColumnConfigurator extends ColumnConfigurator {

    Column column

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        table.addColumn column, ([] as Set)
    }
}
