package org.transmartproject.core.multidimquery

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonSetter
import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class CrossTableRepresentation implements CrossTable {
    List<List<Long>> values

    CrossTableRepresentation() {
        this.values = []
    }

    CrossTableRepresentation(List<CrossTableRowRepresentation> rows) {
        this.values = rows.collect { it.counts }
    }

    @JsonGetter('rows')
    List<List<Long>> getValues() {
        this.values
    }

    @JsonSetter('rows')
    void setValues(List<List<Long>> values) {
        this.values = values
    }

    List<CrossTableRowRepresentation> getRows() {
        values.collect { new CrossTableRowRepresentation(it) }
    }

}

@CompileStatic
@Canonical
class CrossTableRowRepresentation implements CrossTableRow {
    List<Long> counts
}
