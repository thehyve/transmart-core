package org.transmartproject.core.multidimquery.crosstable

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.query.Constraint

@CompileStatic
@Canonical
class CrossTableRequest {
    List<Constraint> rowConstraints
    List<Constraint> columnConstraints
    Constraint subjectConstraint
}
