package jobs.steps.helpers

import jobs.table.Column
import jobs.table.columns.SimpleConceptVariableColumn
import jobs.table.columns.TransformColumnDecorator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.clinical.ClinicalVariable

@Component
@Scope('prototype')
class NumericColumnConfigurator extends ColumnConfigurator {

    public static final String CLINICAL_DATA_TYPE_VALUE = 'CLINICAL'

    String projection             /* only applicable for high dim data */

    String keyForConceptPath,
           keyForDataType,        /* CLINICAL for clinical data */
           keyForSearchKeywordId, /* only applicable for high dim data */
           keyForLog10

    boolean multiRow = false      /* only applicable for high dim data */

    boolean alwaysClinical = false

    @Autowired
    private ClinicalDataRetriever clinicalDataRetriever

    @Autowired
    private ResultInstanceIdsHolder resultInstanceIdsHolder

    @Autowired
    private HighDimensionColumnConfigurator highDimensionColumnConfigurator

    @Override
    protected void doAddColumn(Closure<Column> columnDecorator) {
        def resultColumnDecoretor = log10 ?
                compose(columnDecorator, createLog10ColumnDecorator())
                : columnDecorator
        if (isClinical()) {
            addColumnClinical resultColumnDecoretor
        } else {
            addColumnHighDim resultColumnDecoretor
        }
    }

    private Closure<Column> createLog10ColumnDecorator() {
        { Column originalColumn ->
            new TransformColumnDecorator(
                    inner: originalColumn,
                    valueFunction: { value ->
                        Math.log10(value)
                    })
        }
    }

    boolean isLog10() {
        getStringParam(keyForLog10, false)?.toBoolean()
    }

    boolean isClinical() {
        return alwaysClinical || getStringParam(keyForDataType) == CLINICAL_DATA_TYPE_VALUE
    }

    private void addColumnHighDim(Closure<Column> decorateColumn) {
        ['header', 'projection', 'keyForConceptPath', 'keyForDataType',
                'keyForSearchKeywordId', 'multiRow'].each { prop ->
            highDimensionColumnConfigurator."$prop" = this."$prop"
        }
        highDimensionColumnConfigurator.addColumn decorateColumn
    }

    private void addColumnClinical(Closure<Column> decorateColumn) {
        ClinicalVariable variable = clinicalDataRetriever.
                createVariableFromConceptPath getStringParam(keyForConceptPath).trim()
        variable = clinicalDataRetriever << variable

        clinicalDataRetriever.attachToTable table

        table.addColumn(
                decorateColumn.call(
                        new SimpleConceptVariableColumn(
                                column:      variable,
                                numbersOnly: true,
                                header:      header)),
                [ClinicalDataRetriever.DATA_SOURCE_NAME] as Set)
    }

    /**
     * Sets parameter keys based on optional base key part
     * @param keyPart
     */
    void setKeys(String keyPart = '') {
        keyForConceptPath     = "${keyPart}Variable"
        keyForDataType        = "div${keyPart.capitalize()}VariableType"
        keyForSearchKeywordId = "div${keyPart.capitalize()}VariablePathway"
        keyForLog10           = "div${keyPart.capitalize()}VariableLog10"
    }

}
