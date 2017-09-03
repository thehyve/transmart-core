package org.transmartproject.core.dataquery

interface TypeAwareDataColumn<T> extends DataColumn {

    /**
     * A type of column values.
     *
     * @return java class characterising the column values type.
     */
    Class<T> getType()
}
