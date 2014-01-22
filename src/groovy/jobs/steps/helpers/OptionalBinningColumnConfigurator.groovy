package jobs.steps.helpers

import groovy.util.logging.Log4j
import jobs.table.Column
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

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

    private void setupInnerConfigurator() {
        if (getStringParam(keyForConceptPaths).contains('|')) {
            log.debug("Found pipe character in $keyForConceptPaths, " +
                    "assuming categorical data")

            innerConfigurator = appCtx.getBean CategoricalColumnConfigurator

            innerConfigurator.columnHeader       = getColumnHeader()
            innerConfigurator.keyForConceptPaths = keyForConceptPaths
        } else {
            log.debug("Did not find pipe character in $keyForConceptPaths, " +
                    "assuming continuous data")

            innerConfigurator = appCtx.getBean NumericColumnConfigurator

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

        binningConfigurator.addColumn decorateColumn
    }
}
