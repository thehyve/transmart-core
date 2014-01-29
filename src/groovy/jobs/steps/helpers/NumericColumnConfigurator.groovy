package jobs.steps.helpers

import jobs.table.Column
import jobs.table.columns.SimpleConceptVariableColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.clinical.ClinicalVariable

@Component
@Scope('prototype')
class NumericColumnConfigurator extends ColumnConfigurator {

    public static final String CLINICAL_DATA_TYPE_VALUE = 'CLINICAL'

    String columnHeader

    String projection             /* only applicable for high dim data */

    String keyForConceptPath,
           keyForDataType,        /* CLINICAL for clinical data */
           keyForSearchKeywordId  /* only applicable for high dim data */

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
        if (isClinical()) {
            addColumnClinical columnDecorator
        } else {
            addColumnHighDim columnDecorator
        }
    }

    boolean isClinical() {
        return alwaysClinical || getStringParam(keyForDataType) == CLINICAL_DATA_TYPE_VALUE
    }

    private void addColumnHighDim(Closure<Column> decorateColumn) {
        ['columnHeader', 'projection', 'keyForConceptPath', 'keyForDataType',
                'keyForSearchKeywordId', 'multiRow'].each { prop ->
            highDimensionColumnConfigurator."$prop" = this."$prop"
        }
        highDimensionColumnConfigurator.addColumn decorateColumn
    }

    private void addColumnClinical(Closure<Column> decorateColumn) {
        ClinicalVariable variable = clinicalDataRetriever.
                createVariableFromConceptPath getStringParam(keyForConceptPath).trim()
        clinicalDataRetriever << variable

        clinicalDataRetriever.attachToTable table

        table.addColumn(
                decorateColumn.call(
                        new SimpleConceptVariableColumn(
                                column:      variable,
                                numbersOnly: true,
                                header:      columnHeader)),
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
    }

}
