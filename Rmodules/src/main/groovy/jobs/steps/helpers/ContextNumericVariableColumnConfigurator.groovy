package jobs.steps.helpers

import groovy.util.logging.Log4j
import jobs.table.Column
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

/**
 * A decorator that configures a column whose values will always be maps.
 *
 * Delegates to either a
 * - MultiNumericClinicalVariableColumnConfigurator (clinical concepts)
 * - HighDimensionColumnConfigurator (high dimensional concepts)
 *
 *
 * - Clinical concept:           values are map (possibly pruned concept path -> value)
 * - Single high dim concept^:   values are map (row label (probe) -> numeric value)
 *                               OR, if multiConcepts are set,
 *                               values are map (concept path + '|' + row label (probe) -> value)
 * - Multiple high dim concept+: values are map (concept path + '|' + row label (probe) -> value)
 *
 * ^ if multiRow is set, otherwise values are numeric (first row only)
 * + accepted only if multiConcepts is set
 *
 * multiRow is set by default
 */
@Component
@Scope('prototype')
@Log4j
class ContextNumericVariableColumnConfigurator extends ColumnConfigurator {

    /* properties to be set externally:
     *
     * - header
     * - projection
     * - keyForConceptPaths/keyForConceptPath (indifferent which)
     * - keyForDataType
     * - keyForSearchKeywordId
     * - multiRow
     * - multiConcepts
     */

    @Autowired
    private MultiNumericClinicalVariableColumnConfigurator multiClinicalConfigurator

    @Autowired
    private HighDimensionColumnConfigurator multiHighDimConfigurator

    @PostConstruct
    void init() {
        multiRow = true
    }

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        if (clinicalData) {
            log.debug("$keyForDataType indicates clinical data; " +
                    "using the MultiNumericClinicalVariableColumnConfigurator")
            multiClinicalConfigurator.doAddColumn decorateColumn
        } else {
            log.debug("$keyForDataType indicates high dim data; " +
                    "using the HighDimensionColumnConfigurator")
            multiHighDimConfigurator.doAddColumn decorateColumn
        }
    }

    String getKeyForDataType() {
        multiHighDimConfigurator.keyForDataType
    }


    List<String> getConceptPaths() {
        multiHighDimConfigurator.conceptPaths
    }

    boolean isClinicalData() {
        getStringParam(keyForDataType) ==
                NumericColumnConfigurator.CLINICAL_DATA_TYPE_VALUE
    }

    void setProperty(String name, Object value) {
        if (name == 'keyForConceptPath' || name == 'keyForConceptPaths') {
            multiClinicalConfigurator.keyForConceptPaths = value
            multiHighDimConfigurator.keyForConceptPath   = value
            return
        }

        boolean found = false
        if (multiClinicalConfigurator.hasProperty(name)) {
            multiClinicalConfigurator.setProperty name, value
            found = true
        }
        if (multiHighDimConfigurator.hasProperty(name)) {
            multiHighDimConfigurator.setProperty name, value
            found = true
        }
        if (!found) {
            throw new MissingPropertyException(name, getClass())
        }
    }
}
