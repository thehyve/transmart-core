package jobs.steps.helpers

import jobs.UserParameters
import jobs.table.Column
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

/*
 * BoxPlot has two particularities:
 *
 * - The column with header 'X' should always be the categorical (or binned
 *   numerical) variable
 * - We allow multiple numeric clinical variables.
 */
@Component
@Scope('prototype')
@Qualifier('boxPlot')
class BoxPlotVariableColumnConfigurator extends OptionalBinningColumnConfigurator {

    String categoricalColumnHeader       = 'X'
    String numericColumnHeader           = 'Y'
    String keyForBinnedVariable          = 'binVariable'
    String valueForThisColumnBeingBinned

    @PostConstruct
    void initBoxPlot() {
        binningConfigurator.additionalEnablingCheck = { UserParameters params ->
            getStringParam(keyForBinnedVariable) == valueForThisColumnBeingBinned
        }
        forceNumericBinning             = false
    }

    @Override
    void doAddColumn(Closure<Column> decorateColumn) {
        /* if we have only one variable, we don't necessary want maps as values
         * (namely, we don't want them if we have a clinical variable). */
        numericColumnConfigurationClass =
                isMultiVariable() ?
                        ContextNumericVariableColumnConfigurator :
                        NumericColumnConfigurator

        super.doAddColumn decorateColumn
    }

    @Override
    void setHeader(String header) {
        throw new UnsupportedOperationException(
                'Column header is automatically assigned')
    }

    @Override
    String getHeader() {
        categoricalOrBinned ?
                categoricalColumnHeader :
                numericColumnHeader
    }
}
