package org.transmartproject.core.multidimensionalquery

import org.transmartproject.core.IterableResult
import org.transmartproject.core.ontology.Study

interface Dimension {

    enum Size {
        SMALL,
        MEDIUM,
        LARGE
    }

    enum Density {
        DENSE,
        SPARSE
    }

    enum Packable {
        PACKABLE(packable: true),
        NOT_PACKABLE(packable: false);

        boolean packable
    }


    Size getSize()

    Density getDensity()

    Packable getPackable()

    IterableResult<Object> getElements(Collection<Study> studies)

    List<Object> resolveElements(List elementKeys)

    def resolveElement(elementId)
}