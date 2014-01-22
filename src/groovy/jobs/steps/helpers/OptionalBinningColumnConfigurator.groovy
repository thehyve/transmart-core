package jobs.steps.helpers

import groovy.util.logging.Log4j
import jobs.table.Column
import jobs.table.MissingValueAction
import jobs.table.columns.ConstantValueColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection

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

    private ColumnConfigurator innerConfigurator

    @Autowired
    ApplicationContext appCtx

    private void setupInnerConfigurator(String conceptPaths) {

        boolean emptyConcept = (conceptPaths == '')

        if (emptyConcept) {
            //when required we will never reach here
            log.debug("Not required and no value for $keyForConceptPaths, " +
                    "assuming constant value column")

            innerConfigurator = appCtx.getBean SimpleAddColumnConfigurator
            innerConfigurator.column = new ConstantValueColumn(header: columnHeader, missingValueAction: missingValueAction)
            //innerConfigurator.missingValueAction = missingValueAction
            //table.addColumn(new ConstantValueColumn(header: columnHeader, missingValueAction: missingValueAction), Collections.emptySet())
            innerConfigurator.addColumn()

        } else if (conceptPaths.contains('|')) {
            log.debug("Found pipe character in $keyForConceptPaths, " +
                    "assuming categorical data")

            setupCategorical()
        } else {
            log.debug("Did not find pipe character in $keyForConceptPaths, " +
                    "assuming continuous data")

            setupContinuous()
        }

        if (!emptyConcept) {
            binningConfigurator.innerConfigurator = innerConfigurator
        }
    }

    private void setupCategorical() {
        innerConfigurator = appCtx.getBean CategoricalColumnConfigurator

        innerConfigurator.columnHeader       = getColumnHeader()
        innerConfigurator.keyForConceptPaths = keyForConceptPaths
    }

    private void setupContinuous() {
        innerConfigurator = appCtx.getBean NumericColumnConfigurator

        innerConfigurator.columnHeader          = getColumnHeader()
        innerConfigurator.projection            = projection
        innerConfigurator.keyForConceptPath     = keyForConceptPaths
        innerConfigurator.keyForDataType        = keyForDataType
        innerConfigurator.keyForSearchKeywordId = keyForSearchKeywordId
        innerConfigurator.multiRow              = multiRow
    }

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {

        //if required will fail on empty conceptPaths
        def conceptPaths = getStringParam(keyForConceptPaths, required)

        setupInnerConfigurator(conceptPaths)

        if (conceptPaths != '') {
            //configure binning only if has variable
            binningConfigurator.addColumn decorateColumn
        }
    }

    /**
     * Sets parameter keys based on optional base key part
     * @param keyPart
     */
    void setKeys(String keyPart = '') {
        keyForConceptPaths    = "${keyPart}Variable"
        keyForDataType        = "div${keyPart.capitalize()}VariableType"
        keyForSearchKeywordId = "div${keyPart.capitalize()}VariablePathway"
    }
}
