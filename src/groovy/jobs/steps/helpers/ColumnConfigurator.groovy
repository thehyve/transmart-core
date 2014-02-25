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

    String header

    MissingValueAction missingValueAction

    @Deprecated // use OptionalBinningColumnDecorator instead
    boolean required = true

    final void addColumn() {
        addColumn Closure.IDENTITY
    }

    void addColumn(Closure<Column> decorateColumn) {
        if (!missingValueAction) {
            doAddColumn decorateColumn
        } else {
            /* we don't do the same sort of thing for header because, unlike the
             * MissingValueAction, which only matters on the outer layer (i.e.,
             * the Column that is actually added to the table, not its eventual
             * inner columns), the header has to be propagated all over the
             * place, at least under the current scheme of things. Let's take
             * the example of a binning column decorator wrapping a simple
             * numeric column. The decorator actually delegates getHeader() to
             * the numeric column, which means the numeric column, which is the
             * innermost column, needs to have its header set. Wrapping the
             * outermost column (the binning column decorator) in a decorator
             * like ReplaceMissingValueActionDecorator() would not help because
             * this replacement happens on the outside of the binning column.
             *
             * In essence, the problem is that header serves two purposes. For
             * the outside (i.e., Table), it is the header of the column to be
             * put on the first line of the CSV. But, unlike the missing value
             * action, it is also used internally by the binning decorators
             * to build the values of the column. Therefore, it is not
             * sufficient for the header value to be available only to the
             * outside via a decorator.
             */
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

        if (v && !(v instanceof String)) {
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
