package org.transmartproject.db.dataquery.clinical.variables

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.core.dataquery.DataColumn

@EqualsAndHashCode(includes = [ 'conceptCode', 'conceptPath' ])
@ToString
class TerminalConceptVariable implements TerminalClinicalVariable, DataColumn {

    public final static String GROUP_NAME = this.simpleName

    /* when created, only one needs to be filled, but then a postprocessing
     * step must fill the other */
    String conceptCode,
           conceptPath

    @Override
    String getLabel() {
        conceptPath
    }

    @Override
    String getGroup() {
        GROUP_NAME
    }

    @Override
    String getCode() {
        conceptCode
    }
}
