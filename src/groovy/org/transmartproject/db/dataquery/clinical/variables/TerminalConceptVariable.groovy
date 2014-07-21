package org.transmartproject.db.dataquery.clinical.variables

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn

@EqualsAndHashCode(includes = [ 'conceptCode', 'conceptPath' ])
@ToString
class TerminalConceptVariable implements ClinicalVariableColumn {

    /* when created, only one needs to be filled, but then a postprocessing
     * step must fill the other */
    String conceptCode,
           conceptPath

    @Override
    String getLabel() {
        conceptPath
    }
}
