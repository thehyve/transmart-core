package org.transmartproject.core.pedigree

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@Canonical
@CompileStatic
class RelationRepresentation {

    Long leftSubjectId
    String relationTypeLabel
    Long rightSubjectId
    Boolean biological
    Boolean shareHousehold

}
