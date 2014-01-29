package jobs.steps.helpers

import jobs.UserParameters
import jobs.table.Column
import jobs.table.MissingValueAction
import jobs.table.Table
import jobs.table.columns.ColumnDecorator
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException

abstract class ColumnConfigurator {

    @Autowired
    private UserParameters params

    @Autowired
    private Table table

    MissingValueAction missingValueAction

    boolean required = true

    final void addColumn() {
        addColumn Closure.IDENTITY
    }

    void addColumn(Closure<Column> decorateColumn) {
        if (!missingValueAction) {
            doAddColumn decorateColumn
        } else {
            doAddColumn(compose(
                    { Column it ->
                        new ReplaceMissingValueActionDecorator(
                                inner:              it,
                                missingValueAction: missingValueAction)
                    },
                    decorateColumn))
        }
    }

    static class ReplaceMissingValueActionDecorator implements ColumnDecorator {
        @Delegate
        Column inner

        private MissingValueAction missingValueAction

        void setMissingValueAction(MissingValueAction missingValueAction) {
            this.missingValueAction = missingValueAction
        }

        // @Delegate doesn't seem to work well with overriding properties
        // when using the simple syntax "MissingValueAction missingValueAction"
        MissingValueAction getMissingValueAction() {
            this.missingValueAction
        }
    }

    abstract protected void doAddColumn(Closure<Column> decorateColumn)

    protected final String getStringParam(String key, boolean required = true) {
        def v = params[key]
        if (!v && required) {
            throw new InvalidArgumentsException("The parameter '$key' has not " +
                    "been provided by the client")
        }

        if (!(v instanceof String)) {
            throw new InvalidArgumentsException("Expected the parameter '$key' " +
                    "to be a String, got a ${v.getClass()}")
        }

        v
    }

    protected  compose(Closure<Column> externalDecorate, Closure<Column> ourDecorate) {
        { Column orig ->
            externalDecorate.call(ourDecorate.call(orig))
        }
    }

    final protected getParams() {
        params
    }

    final protected getTable() {
        table
    }

}
