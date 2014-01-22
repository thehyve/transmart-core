package jobs.steps.helpers

import jobs.UserParameters
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

/*
 * BoxPlot has two particularities:
 *
 * - Whether the binning should be made on any specific column depends on the
 *   value of the 'binVariable'
 * - The column with header 'X' should always be the categorical (or binned
 *   numerical) variable
 */
@Component
@Scope('prototype')
class BoxPlotVariableColumnConfigurator extends CategoricalOrBinnedColumnConfigurator {

    String categoricalColumnHeader       = 'X'
    String numericColumnHeader           = 'Y'
    String keyForBinnedVariable          = 'binVariable'
    String valueForThisColumnBeingBinned

    @PostConstruct
    void initBoxPlot() {
        binningConfigurator.additionalEnablingCheck = { UserParameters params ->
            getStringParam(keyForBinnedVariable) == valueForThisColumnBeingBinned
        }
    }

    void setColumnHeader(String header) {
        throw new UnsupportedOperationException(
                'Column header is automatically assigned')
    }

    String getColumnHeader() {
        categorical ?
                categoricalColumnHeader :
                numericColumnHeader
    }

    boolean isCategorical() {
        getStringParam(keyForConceptPaths).contains('|') || /* is categorical */
                binningConfigurator.binningEnabled
    }
}
