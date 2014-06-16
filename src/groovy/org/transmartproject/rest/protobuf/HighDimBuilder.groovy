/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.protobuf

import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.rest.protobuf.HighDimProtos.Assay
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimHeader
import org.transmartproject.rest.protobuf.HighDimProtos.ColumnSpec
import org.transmartproject.rest.protobuf.HighDimProtos.ColumnSpec.ColumnType
import org.transmartproject.rest.protobuf.HighDimProtos.Row
import org.transmartproject.rest.protobuf.HighDimProtos.ColumnValue

/**
 * Helper class to build HighDimTable for highdim results
 * For performance reasons, this builder has state and cannot be reused, so needs a new instance for every TabularResult
 */
@CompileStatic
class HighDimBuilder {

    /**
     * The projection used (mandatory)
     */
    Projection projection

    List<AssayColumn> assayColumns

    OutputStream out

    @Lazy private Map<String, ? extends Class> dataColumns = { getDataProperties(projection) }()

    private Row.Builder rowBuilder = Row.newBuilder()
    private ColumnValue.Builder columnBuilder = ColumnValue.newBuilder()

    static void write(Projection projection,
                                 TabularResult<AssayColumn, DataRow<AssayColumn,? extends Object>> tabularResult,
                                 OutputStream out) {

        Iterator<DataRow<AssayColumn,?>> rows = tabularResult.getRows()
        if (!rows.hasNext()) {
            throw new IllegalArgumentException("No results") //TODO: improve this
        }

        List<AssayColumn> cols = tabularResult.indicesList

        HighDimBuilder builder = new HighDimBuilder(
                projection: projection,
                assayColumns: cols,
                out: out,
        )

        builder.createHeader().writeDelimitedTo(out)

        // Normal groovy iterator syntax doesn't seem to work in this case with CompileStatic
        while(rows.hasNext()) {
            builder.createRow(rows.next()).writeDelimitedTo(out)
        }
    }

    private HighDimHeader createHeader() {
        HighDimHeader.Builder headerBuilder = HighDimHeader.newBuilder()
        ColumnSpec.Builder specBuilder = ColumnSpec.newBuilder()

        headerBuilder.addAllColumnSpec(dataColumns.collect { String name, Class cl ->
            specBuilder.clear()
            specBuilder.setName(safeString(name))
            specBuilder.setType(typeForClass(cl))
            specBuilder.build()
        })

        Assay.Builder assayBuilder = Assay.newBuilder()
        headerBuilder.addAllAssay( assayColumns.collect { AssayColumn col ->
            createAssay(assayBuilder, col)
        })

        headerBuilder.build()
    }

    public static ColumnType typeForClass(Class clazz) {
        clazz in [Double, BigDecimal] ? ColumnType.DOUBLE : ColumnType.STRING
    }

    public static Map<String, Class> getDataProperties(Projection projection) {
        if (projection instanceof MultiValueProjection) {
            MultiValueProjection mvp = projection as MultiValueProjection
            return mvp.dataProperties
        } else {
            return [('value'): Double]
        }
    }

    private Row createRow(DataRow<AssayColumn, Object> inputRow) {
        rowBuilder.clear()
        rowBuilder.setLabel(inputRow.label)
        if (inputRow instanceof BioMarkerDataRow) {
            rowBuilder.setBioMarker(safeString(((BioMarkerDataRow) inputRow).bioMarker))
        }

        if (isMultiValuedProjection()) {
            Map<String, ColumnValue.Builder> cols = (Map) dataColumns.collectEntries { col, type ->
                [(col): ColumnValue.newBuilder()]
            }

            // transpose the data row
            for (AssayColumn a : assayColumns) {
                def obj = inputRow.getAt(a) //can be a map or some bean

                dataColumns.each { String col, Class type ->
                    def value = obj.getAt(col)
                    if(typeForClass(type) == ColumnType.DOUBLE) {
                        cols[col].addDoubleValue(value as Double) //we must convert explicitly to Double
                    } else {
                        cols[col].addStringValue(safeString(value))
                    }
                }
            }

            dataColumns.each { String col, Class type ->
                rowBuilder.addValue(cols[col])
            }
        } else {
            // Single column projection
            columnBuilder.clear()
            columnBuilder.addAllDoubleValue(assayColumns.collect { AssayColumn col -> inputRow.getAt(col) as Double})
            rowBuilder.addValue(columnBuilder)
        }

        rowBuilder.build()
    }

    private Assay createAssay(Assay.Builder builder, AssayColumn col) {
        builder.clear()
        builder.assayId = col.id
        builder.patientId = safeString(col.patientInTrialId)

        Map optionalValues = [
                // Capitalized because we're pasting 'set' in front of the key strings
                Platform: col.platform?.id,
                SampleCode: col.sampleCode,
                SampleTypeName: col.sampleType?.label,
                TimepointName: col.timepoint?.label,
                TissueTypeName: col.tissueType?.label,
        ]
        
        optionalValues.each { field, value ->
            if(value != null) {
                // Java reflection because of CompileStatic. The conversion to Object[] is not necessary,
                // but otherwise IntelliJ shows an error
                builder.class.getDeclaredMethod('set'+field, String).invoke(builder, [value] as Object[])
            }
        }
        builder.build()
    }

    private boolean isMultiValuedProjection() {
        projection instanceof MultiValueProjection
    }

    public static String safeString(Object obj) {
        (obj == null) ? '' : obj.toString()
    }

}
