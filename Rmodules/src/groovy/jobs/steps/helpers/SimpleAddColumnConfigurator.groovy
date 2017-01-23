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
        if (header && header != column.header) {
            throw new IllegalStateException('This configurator cannot ' +
                    'control the resulting column\'s header')
        }
        table.addColumn column, ([] as Set)
    }
}
