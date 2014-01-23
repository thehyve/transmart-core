package jobs.steps.helpers

import groovy.util.logging.Log4j
import jobs.table.Column
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidArgumentsException

/**
 * A configurator supporting:
 *
 * - categorical variables, binned or not
 * - low dimensional data, binned or not
 *   (no binning allowed only if !forceNumericBinning)
 * - high dimensional data, multirow or not (depending on multiRow),
 *   binning or not (no binning allowed only if !forceNumericBinning)
 */
@Log4j
@Component
@Scope('prototype')
@Qualifier('general')
class OptionalBinningColumnConfigurator extends ColumnConfigurator {

    @Autowired
    BinningColumnConfigurator binningConfigurator

    String columnHeader

    String projection

    String keyForConceptPaths,
           keyForDataType,        /* CLINICAL for clinical data */
           keyForSearchKeywordId  /* only applicable for high dim data */

    boolean multiRow = false

    boolean forceNumericBinning = true

    protected Class<? extends ColumnConfigurator> numericColumnConfigurationClass =
            NumericColumnConfigurator

    private ColumnConfigurator innerConfigurator

    @Autowired
    ApplicationContext appCtx

    private void setupInnerConfigurator() {
        if (categorical) {
            log.debug("Found pipe character in $keyForConceptPaths, " +
                    "assuming categorical data")

            innerConfigurator = appCtx.getBean CategoricalColumnConfigurator

            innerConfigurator.columnHeader       = getColumnHeader()
            innerConfigurator.keyForConceptPaths = keyForConceptPaths
        } else {
            log.debug("Did not find pipe character in $keyForConceptPaths, " +
                    "assuming continuous data")

            innerConfigurator = appCtx.getBean numericColumnConfigurationClass

            innerConfigurator.columnHeader          = getColumnHeader()
            innerConfigurator.projection            = projection
            innerConfigurator.keyForConceptPath     = keyForConceptPaths
            innerConfigurator.keyForDataType        = keyForDataType
            innerConfigurator.keyForSearchKeywordId = keyForSearchKeywordId
            innerConfigurator.multiRow              = multiRow
        }

        binningConfigurator.innerConfigurator = innerConfigurator
    }

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        setupInnerConfigurator()

        if (!binningConfigurator.binningEnabled &&
                !(innerConfigurator instanceof CategoricalColumnConfigurator) &&
                forceNumericBinning) {
            throw new InvalidArgumentsException("Numeric variables must be " +
                    "binned for column $columnHeader")
        }

        binningConfigurator.addColumn decorateColumn
    }

    boolean isCategorical() {
        getStringParam(keyForConceptPaths).contains('|')
    }

    boolean isCategoricalOrBinned() {
        isCategorical() || binningConfigurator.binningEnabled
    }
}
