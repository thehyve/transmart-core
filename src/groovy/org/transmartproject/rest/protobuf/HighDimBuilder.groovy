package org.transmartproject.rest.protobuf

import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
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

    //no bioMarkerClosure for DataRowType.REGION
    @Lazy private Closure<String> bioMarkerClosure =  (DataRowType.REGION == dataRowType) ? null : this.&getBioMarkerLabel

    @Lazy private Map<String, Class> dataColumns = {

        if (rowType == RowType.DOUBLE) {
            return [:] //not needed for double rows
        } else if (projection instanceof MultiValueProjection) {
            MultiValueProjection mvp = projection as MultiValueProjection
            return mvp.dataProperties
        } else {
            throw new IllegalArgumentException("Not supported for $projection")
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
        headerBuilder.addAllStringColumn(dataColumns.collectMany({col, type -> type == String ? [col] : []}))
        headerBuilder.addAllDoubleColumn(dataColumns.collectMany({col, type -> type == Double ? [col] : []}))
        Assay.Builder assayBuilder = Assay.newBuilder()
        headerBuilder.addAllAssay( assayColumns.collect { createAssay(assayBuilder, it) } )
        headerBuilder.build()
    }

    private Row createRow(DataRow<AssayColumn, Object> inputRow) {
        rowBuilder.clear()
        rowBuilder.setLabel(inputRow.label)
        if (bioMarkerClosure) {
            rowBuilder.setBioMarker(safeString(bioMarkerClosure(inputRow)))
        }

        for (AssayColumn col: assayColumns) {
            rowFiller(inputRow.getAt(col))
        }

        rowBuilder.build()
    }

    private MapValue valueAsMapValue(Object obj) {

        mapValueBuilder.clear()

        if (obj != null) {
            dataColumns.each { col, type ->
                switch(type) {
                    case Double:
                        mapValueBuilder.addDoubleValue(obj[col])
                        break
                    default:
                        mapValueBuilder.addStringValue(safeString(obj[col]))
                        break
                }
            }
        }

        mapValueBuilder.build()
    }

    private Assay createAssay(Assay.Builder builder, AssayColumn col) {
        builder.clear()
        builder.assayId = col.id
        builder.patientId = safeString(col.patientInTrialId)

        Map optionalValues = [
                platform: col.platform?.id,
                sampleCode: col.sampleCode,
                sampleTypeName: col.sampleType?.label,
                timepointName: col.timepoint?.label,
                tissueTypeName: col.tissueType?.label,
        ]
        
        optionalValues.each { field, value ->
            if(value != null) {
                builder."$field" = value
            }
        }
        builder.build()
    }

    private void fillDoubleRow(Object value) {
        double dbl = value == null ? Double.NaN : value as Double
        rowBuilder.addDoubleValue(dbl)
    }

    private void fillMapRow(Object value) {
        rowBuilder.addMapValue(valueAsMapValue(value))
    }

    private String getBioMarkerLabel(BioMarkerDataRow<?> inputRow) {
        inputRow.bioMarker
    }

    private static String safeString(Object obj) {
        (obj == null) ? '' : obj.toString()
    }

}
