package org.transmartproject.rest.protobuf

import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.rest.protobuf.HighDimProtos.Assay
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimHeader
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimHeader.RowType
import org.transmartproject.rest.protobuf.HighDimProtos.MapValue
import org.transmartproject.rest.protobuf.HighDimProtos.Row

/**
 * Helper class to build HighDimTable for highdim results
 * For performance reasons, this builder has state and cannot be reused, so needs a new instance for every TabularResult
 */
//@groovy.transform.CompileStatic
class HighDimBuilder {

    static enum DataRowType {
        BIO_MARKER,
        REGION,

        static DataRowType forRow(DataRow<?,?> row) {
            //assumes all non biomarker is region
            return (row instanceof BioMarkerDataRow) ? BIO_MARKER : REGION
        }
    }

    /**
     * The projection used (mandatory)
     */
    Projection projection

    /**
     * The projection used (mandatory)
     */
    DataRowType dataRowType

    RowType rowType

    List<AssayColumn> assayColumns

    OutputStream out

    @Lazy private Closure rowFiller = (RowType.DOUBLE == rowType) ? this.&fillDoubleRow : this.&fillMapRow

    private Closure<String> bioMarkerClosure =  (DataRowType.REGION == dataRowType) ? { null } : this.&getBioMarkerLabel

    @Lazy private List<String> dataColumns = {
        if (projection instanceof AllDataProjection) {
            AllDataProjection adp = projection as AllDataProjection
            adp.dataProperties.toList()
        } else {
            []
        }
    }()

    private Row.Builder rowBuilder = Row.newBuilder()
    private MapValue.Builder mapValueBuilder = MapValue.newBuilder()

    static void write(Projection projection,
                                 TabularResult<AssayColumn, DataRow<AssayColumn,? extends Object>> tabularResult,
                                 OutputStream out) {

        PeekingIterator<DataRow<AssayColumn,?>> it = Iterators.peekingIterator(tabularResult.rows)
        if (!it.hasNext()) {
            throw new IllegalArgumentException("No results") //TODO: improve this
        }

        List<AssayColumn> cols = tabularResult.indicesList

        DataRow<AssayColumn,? extends Object> sampleRow = it.peek() //peeks the 1st row, for inspecting the types
        DataRowType dataRowType = DataRowType.forRow(sampleRow)

        Object sampleValue = cols.collect { sampleRow[it] } find { it != null } //gets the 1st non null value
        RowType rowType = (sampleValue instanceof Number) ? RowType.DOUBLE : RowType.GENERAL

        HighDimBuilder builder = new HighDimBuilder(
                projection: projection,
                dataRowType: dataRowType,
                rowType: rowType,
                assayColumns: cols,
                out: out,
        )

        builder.createHeader().writeDelimitedTo(out)

        for (DataRow<AssayColumn, ? extends Object> dataRow: it) {
            builder.createRow(dataRow).writeDelimitedTo(out)
        }
    }

    private HighDimHeader createHeader() {
        HighDimHeader.Builder headerBuilder = HighDimHeader.newBuilder()
        headerBuilder.setRowsType(rowType)
        headerBuilder.addAllMapColumn(dataColumns)
        Assay.Builder assayBuilder = Assay.newBuilder()
        headerBuilder.addAllAssay( assayColumns.collect { createAssay(assayBuilder, it) } )
        headerBuilder.build()
    }

    private Row createRow(DataRow<AssayColumn, Object> inputRow) {
        rowBuilder.clear()
        rowBuilder.setLabel(inputRow.label)
        rowBuilder.setBioMarker(bioMarkerClosure(inputRow))

        for (AssayColumn col: assayColumns) {
            rowFiller(inputRow.getAt(col))
        }

        rowBuilder.build()
    }

    private MapValue valueAsMapValue(Map<String, String> map) {

        mapValueBuilder.clear()
        List<String> values = []

        if (map != null) {
            for (String col: dataColumns) {
                Object value = map[col]
                String str = (value != null) ? String.valueOf(value) : "" // as String doesn't work here
                values << str
            }
        }

        mapValueBuilder.addAllValue(values)
        mapValueBuilder.build()
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

    private void fillDoubleRow(Object value) {
        double dbl = value == null ? Double.NaN : value as Double
        rowBuilder.addDoubleValue(dbl)
    }

    private void fillMapRow(Object value) {
        rowBuilder.addMapValue(valueAsMapValue(value as Map))
    }

    private String getBioMarkerLabel(BioMarkerDataRow<?> inputRow) {
        inputRow.bioMarker
    }

}
