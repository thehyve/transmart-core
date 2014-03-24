package org.transmartproject.rest.protobuf

import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimTable
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimTable.Assay
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimTable.BaseRow
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimTable.DoubleRow
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimTable.MapEntry
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimTable.MapRow
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimTable.RowType

/**
 * Helper class to build HighDimTable for highdim results
 * For performance reasons, this builder has state and cannot be reused, so needs a new instance for every TabularResult
 */
class HighDimTableBuilder {

    static enum DataRowType {
        BIO_MARKER,
        REGION,

        static DataRowType forRow(DataRow row) {
            //assumes all non biomarker is region
            return (row instanceof BioMarkerDataRow) ? BIO_MARKER : REGION
        }
    }

    /**
     * The projection used (mandatory)
     */
    Projection projection

    /**
     * Row type of the data (mandatory)
     */
    DataRowType dataRowType

    /**
     * Value type (mandatory)
     */
    RowType rowType

    /**
     * The assay columns used (mandatory)
     */
    List<AssayColumn> assayColumns

    private Closure rowHandler = (RowType.DOUBLE == rowType) ? doubleRowHandler : mapRowHandler

    private Closure<String> bioMarkerClosure = (DataRowType.REGION == dataRowType) ? notBioMarker : bioMarker

    private HighDimTable.Builder tableBuilder = HighDimTable.newBuilder()
    private BaseRow.Builder baseRowBuilder = BaseRow.newBuilder()
    private DoubleRow.Builder doubleRowBuilder = DoubleRow.newBuilder()
    private MapRow.Builder mapRowBuilder = MapRow.newBuilder()
    private HighDimTable.Map.Builder mapBuilder = HighDimTable.Map.newBuilder()
    private MapEntry.Builder mapEntryBuilder = MapEntry.newBuilder()

    /**
     * Convenience method to create the protobuf HighDimTable object from the highdim results
     * @param projection
     * @param tabularResult
     * @return HighDimTable object
     */
    static HighDimTable buildForTabularResult(Projection projection,
                                              TabularResult<AssayColumn, Object> tabularResult) {

        PeekingIterator<DataRow<AssayColumn, Object>> it = Iterators.peekingIterator(tabularResult)
        if (!it.hasNext()) {
            throw new IllegalArgumentException("No results") //TODO: csilva: review this
        }

        List<AssayColumn> cols = tabularResult.indicesList

        DataRow<AssayColumn, Object> sampleRow = it.peek() //peeks the 1st row, for inspecting the types
        DataRowType dataRowType = DataRowType.forRow(sampleRow)

        Object sampleValue = cols.collect { sampleRow.getAt(it) } find { it != null } //gets the 1st non null value
        RowType rowType = (sampleValue instanceof Number) ? RowType.DOUBLE : RowType.GENERAL

        HighDimTableBuilder builder = new HighDimTableBuilder(
                projection: projection,
                dataRowType: dataRowType,
                rowType: rowType,
                assayColumns: cols,
        )

        for (DataRow<AssayColumn, Object> dataRow: it) {
            builder.addRow(dataRow)
        }

        builder.build()
    }

    HighDimTable build() {
        tableBuilder.setRowsType(rowType)
        tableBuilder.clearAssay()
        Assay.Builder assayBuilder = Assay.newBuilder()
        tableBuilder.addAllAssay( assayColumns.collect { createAssay(assayBuilder, it)} )
    }

    void addRow(DataRow<AssayColumn, Object> inputRow) {
        rowHandler(inputRow)
    }

    Closure doubleRowHandler = { DataRow<AssayColumn, Object> inputRow ->
        tableBuilder.addDoubleRow(createDoubleRow(inputRow))
    }

    Closure mapRowHandler = { DataRow<AssayColumn, Object> inputRow ->
        tableBuilder.addMapRow(createMapRow(inputRow))
    }

    Closure<String> bioMarker = { BioMarkerDataRow<?> inputRow ->
        inputRow.bioMarker
    }

    Closure<String> notBioMarker = { DataRow<?> inputRow ->
        null
    }

    private DoubleRow createDoubleRow(DataRow<AssayColumn, Object> inputRow) {

        doubleRowBuilder.clear()
        doubleRowBuilder.setBase(createBaseRow(inputRow))
        for (AssayColumn col: assayColumns) {
            Map<String, String> data = inputRow.getAt(col)
            doubleRowBuilder.addValue(valueAsDouble(data))
        }
        doubleRowBuilder.build()
    }

    private double valueAsDouble(Object value) {
        value == null ? Double.NaN : value as Double
    }

    private BaseRow createBaseRow(DataRow<AssayColumn, Object> inputRow) {

        baseRowBuilder.clear()
        baseRowBuilder.setLabel(inputRow.label)
        baseRowBuilder.setBioMarker(bioMarkerClosure(inputRow))
        //no extra properties being filled (for now)

        baseRowBuilder.build()
    }

    private MapRow createMapRow(DataRow<AssayColumn, Object> inputRow) {

        mapRowBuilder.clear()
        mapRowBuilder.setBase(createBaseRow(inputRow))
        for (AssayColumn col: assayColumns) {
            Map<String, String> data = inputRow.getAt(col)
            mapRowBuilder.addValue(valueAsMap(data))
        }
        mapRowBuilder.build()
    }

    private HighDimTable.Map valueAsMap(Map<String, String> map) {

        mapEntryBuilder.clear()
        mapBuilder.clear()
        if (map != null) {
            def entries = map.collect {
                mapEntryBuilder.setKey(it.key)
                mapEntryBuilder.setValue(it.value)
                mapEntryBuilder.build()
            }

            mapBuilder.addAllEntry(entries)
         }
        mapBuilder.build()
    }

    private Assay createAssay(Assay.Builder builder, AssayColumn col) {
        builder.clear()
        builder.assayId = col.id
        builder.patientId = col.patientInTrialId
        builder.platform = col.platform.id
        builder.sampleCode = col.sampleCode
        builder.sampleTypeName = col.sampleType.label
        builder.timepointName = col.timepoint.label
        builder.tissueTypeName = col.tissueType.label
        builder.build()
    }
}
