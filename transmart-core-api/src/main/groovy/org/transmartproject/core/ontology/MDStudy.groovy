package org.transmartproject.core.ontology

import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.users.ProtectedResource

interface MDStudy extends ProtectedResource {
    String getName()

    Collection<Dimension> getDimensions()

}
