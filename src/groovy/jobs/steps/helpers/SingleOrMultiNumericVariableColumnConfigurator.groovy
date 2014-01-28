package jobs.steps.helpers

import groovy.util.logging.Log4j
import jobs.table.Column
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

/**
 * Delegates to either a
 * - NumericColumnConfigurator (single concept, high dim or clinical)
 * - MultiNumericClinicalVariableColumnConfigurator (multiple clinical concepts)
 * - HighDimensionColumnConfigurator (multiple high dimensional objects)
 *
 * Note that the different cases will likely have to be handled differently
 * downstream. The values returned by the {@link jobs.table.Table} will be
 * substantially different in several cases:
 *
 * - Single clinical concept:    values are numeric
 * - Single high dim concept^:   values are map (row label (probe) -> numeric value)
 * - Multiple clinical concept:  values are map (possibly pruned concept path -> value)
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
class SingleOrMultiNumericVariableColumnConfigurator extends ColumnConfigurator {

    /* properties to be set externally:
     *
     * - columnHeader
     * - projection
     * - keyForConceptPaths/keyForConceptPath (indifferent which)
     * - keyForDataType
     * - keyForSearchKeywordId
     * - multiRow
     * - multiConcepts
     * - pruneConceptPath
     */

    @Autowired
    private MultiNumericClinicalVariableColumnConfigurator multiConfigurator

    @Autowired
    private NumericColumnConfigurator singleConfigurator

    @Autowired
    private HighDimensionColumnConfigurator multiHighDimConfigurator

    @PostConstruct
    void init() {
        multiRow = true
    }

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        String conceptPaths = getStringParam keyForConceptPaths

        if (conceptPaths.contains('|')) {
            if (clinicalData) {
                log.debug("Found pipe in parameter $keyForConceptPaths, " +
                        "and $keyForDataType indicates clinical data; " +
                        "using the MultiNumericVariableColumnConfigurator")
                multiConfigurator.doAddColumn decorateColumn
            } else {
                log.debug("Found pipe in parameter $keyForConceptPaths, " +
                        "and $keyForDataType indicates high dim data; " +
                        "using the HighDimensionColumnConfigurator")
                multiHighDimConfigurator.doAddColumn decorateColumn
            }
        } else {
            log.debug("Found no pipe in parameter $keyForConceptPaths, " +
                    "using the NumericColumnConfigurator")
            singleConfigurator.doAddColumn decorateColumn
        }
    }

    String getKeyForConceptPaths() {
        multiConfigurator.keyForConceptPaths // could be fetched anywhere else
    }

    String getKeyForDataType() {
        singleConfigurator.keyForDataType
    }

    boolean isClinicalData() {
        getStringParam(keyForDataType) ==
                NumericColumnConfigurator.CLINICAL_DATA_TYPE_VALUE
    }

    void setProperty(String name, Object value) {
        if (name == 'keyForConceptPath' || name == 'keyForConceptPaths') {
            multiConfigurator.keyForConceptPaths       = value
            singleConfigurator.keyForConceptPath       = value
            multiHighDimConfigurator.keyForConceptPath = value
            return
        }

        boolean found = false
        if (multiConfigurator.hasProperty(name)) {
            multiConfigurator.setProperty name, value
            found = true
        }
        if (singleConfigurator.hasProperty(name)) {
            singleConfigurator.setProperty name, value
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
