package jobs.steps.helpers

import groovy.util.logging.Log4j
import jobs.table.Column
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/*
 * Delegates to either a NumericColumnConfigurator or a
 * MultiNumericVariableColumnConfigurator.
 *
 * Hence it supports either a single high dimensional concept or an arbitrary
 * number of numerical low dimensional concepts.
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
     *
     * Basically the same as for NumericColumnConfigurator, plus allowing
     * replacing keyForConceptPath with keyForConceptPaths.
     */

    @Autowired
    private MultiNumericClinicalVariableColumnConfigurator multiConfigurator

    @Autowired
    private NumericColumnConfigurator singleConfigurator

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        String conceptPaths = getStringParam keyForConceptPaths

        if (conceptPaths.contains('|')) {
            log.debug("Found pipe in parameter $keyForConceptPaths, " +
                    "using the MultiNumericVariableColumnConfigurator")
            multiConfigurator.doAddColumn decorateColumn
        } else {
            log.debug("Found no pipe in parameter $keyForConceptPaths, " +
                    "using the NumericColumnConfigurator")
            singleConfigurator.doAddColumn decorateColumn
        }
    }

    String getKeyForConceptPaths() {
        multiConfigurator.keyForConceptPaths // could be fetch from single too
    }

    void setProperty(String name, Object value) {
        if (name == 'keyForConceptPath' || name == 'keyForConceptPaths') {
            multiConfigurator.keyForConceptPaths = value
            singleConfigurator.keyForConceptPath = value
            return
        }

        if (multiConfigurator.hasProperty(name)) {
            multiConfigurator.setProperty name, value
        }
        if (singleConfigurator.hasProperty(name)) {
            singleConfigurator.setProperty name, value
        }
    }
}
