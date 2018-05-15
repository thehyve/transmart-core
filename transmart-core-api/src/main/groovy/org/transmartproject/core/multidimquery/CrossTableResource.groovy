package org.transmartproject.core.multidimquery

import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.User

interface CrossTableResource {
    /**
     *
     * @param rowConstraints - The list of constraints, each specified for a table row
     * @param columnConstraints - The list of constraints, each specified for a table column
     * @param subjectConstraint : The constraint describing the set of patients specified by user
     * @param user : The current user
     * @return the {@link CrossTable} representation
     */
    CrossTable retrieveCrossTable(List<Constraint> rowConstraints, List<Constraint> columnConstraints,
                                                    Constraint subjectConstraint, User user)
}
