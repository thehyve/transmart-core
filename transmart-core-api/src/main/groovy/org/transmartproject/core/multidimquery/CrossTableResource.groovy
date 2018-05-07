package org.transmartproject.core.multidimquery

import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.User

interface CrossTableResource {
    CrossTable retrieveCrossTable(List<Constraint> rowConstraints, List<Constraint> columnConstraints,
                                                    Long patientSetId, User user)
}
