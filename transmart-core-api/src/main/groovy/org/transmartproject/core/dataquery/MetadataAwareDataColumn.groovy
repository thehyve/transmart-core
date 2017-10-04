package org.transmartproject.core.dataquery

/**
 * A data column that contains metadata about itself.
 */
interface MetadataAwareDataColumn extends DataColumn {
    VariableMetadata getMetadata()
}