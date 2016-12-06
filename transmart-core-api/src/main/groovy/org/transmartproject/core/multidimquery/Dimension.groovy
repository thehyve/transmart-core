package org.transmartproject.core.multidimquery

import org.transmartproject.core.IterableResult
import org.transmartproject.core.ontology.Study

interface Dimension {

    /**
     * Indicates the typical size of a dimension. As this is typical, the actual size in a result set does not always
     * match this range.
     *
     * Small: up to 10 to 15 elements
     * Medium: 10 to 50 elements
     * Large: more than 50 elements
     */
    enum Size {
        SMALL,
        MEDIUM,
        LARGE
    }

    enum Density {
        DENSE(isDense: true),
        SPARSE(isDense: false),

        boolean isDense
    }

    enum Packable {
        PACKABLE(packable: true),
        NOT_PACKABLE(packable: false);

        boolean packable
    }


    String getName()

    Size getSize()

    Density getDensity()

    Packable getPackable()

    IterableResult<Object> getElements(Collection<Study> studies)

    List<Object> resolveElements(List elementKeys)

    def resolveElement(elementId)
}