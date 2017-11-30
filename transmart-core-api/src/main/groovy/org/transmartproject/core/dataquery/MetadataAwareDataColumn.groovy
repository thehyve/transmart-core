package org.transmartproject.core.dataquery

import org.transmartproject.core.ontology.VariableMetadata

/**
 * A data column that contains metadata about itself.
 */
interface MetadataAwareDataColumn extends DataColumn {
    VariableMetadata getMetadata()
}