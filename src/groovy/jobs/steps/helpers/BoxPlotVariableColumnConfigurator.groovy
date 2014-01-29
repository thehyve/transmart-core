package jobs.steps.helpers

import jobs.UserParameters
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

/*
 * BoxPlot has three particularities:
 *
 * - Whether the binning should be made on any specific column depends on the
 *   value of the 'binVariable'
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
    String keyForIsCategorical
    String valueForThisColumnBeingBinned

    @PostConstruct
    void initBoxPlot() {
        binningConfigurator.additionalEnablingCheck = { UserParameters params ->
            getStringParam(keyForBinnedVariable) == valueForThisColumnBeingBinned
        }
        forceNumericBinning             = false
        numericColumnConfigurationClass = SingleOrMultiNumericVariableColumnConfigurator
    }

    @Override
    void setColumnHeader(String header) {
        throw new UnsupportedOperationException(
                'Column header is automatically assigned')
    }

    @Override
    String getColumnHeader() {
        categoricalOrBinned ?
                categoricalColumnHeader :
                numericColumnHeader
    }

    @Override
    boolean isCategorical() {
        getStringParam(keyForIsCategorical).equalsIgnoreCase('true')
    }
}
