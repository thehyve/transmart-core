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

import com.google.common.base.Function
import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.Iterators
import com.google.common.collect.Maps
import com.google.common.collect.PeekingIterator
import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.rest.protobuf.HighDimProtos.Assay
import org.transmartproject.rest.protobuf.HighDimProtos.ColumnSpec
import org.transmartproject.rest.protobuf.HighDimProtos.ColumnSpec.ColumnType
import org.transmartproject.rest.protobuf.HighDimProtos.ColumnValue
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimHeader
import org.transmartproject.rest.protobuf.HighDimProtos.Row

/**
 * Helper class to build HighDimTable for highdim results
 * For performance reasons, this builder has state and cannot be reused, so needs a new instance for every TabularResult
 */
@CompileStatic
class HighDimBuilder {

    private Projection projection

    private TabularResult<AssayColumn, ?> tabularResult

    private PeekingIterator<DataRow<AssayColumn, ?>> rowsIterator

    private Row.Builder rowBuilder = Row.newBuilder()

    @Lazy boolean multiValuedProjection = projection instanceof MultiValueProjection

    // class is either Double or String
    @Lazy private SortedMap<String, ? extends Class> dataColumns = getDataProperties(projection)

    // closures that take a data row and return a column builder
    // each closure is associated with a column
    // the order is that of the columns as given in the sorted map dataColumns
    @Lazy private List<Closure> columnValueBuilderCreators = {
        dataColumns.collect { String dataProperty, Class clazz ->
            // the builder will be reused across rows
            def builder = ColumnValue.newBuilder()
            if (clazz == Double) {
                this.&columnValueBuilderSetupForDouble.curry(builder,
                        multiValuedProjection ? dataProperty : null)
            } else {
                this.&columnValueBuilderSetupForString.curry(builder,
                        multiValuedProjection ? dataProperty : null)
            }
        }
    }()

    HighDimBuilder(Projection projection,
                   TabularResult<AssayColumn, ?> tabularResult) {
        this.projection = projection
        this.tabularResult = tabularResult
        this.rowsIterator = Iterators.peekingIterator(tabularResult.getRows()) as PeekingIterator

        if (!this.rowsIterator.hasNext()) {
            throw new EmptySetException('No data')
        }
    }

    private HighDimProtos.ColumnValue.Builder columnValueBuilderSetupForDouble(
            HighDimProtos.ColumnValue.Builder builder, String dataProperty, DataRow<AssayColumn, ?> row) {
        builder.clear()
        builder.addAllDoubleValue tabularResult.indicesList.collect { AssayColumn col ->
            def cell = row.getAt col
            if (dataProperty) { // multi value projection
                cell = cell?.getAt(dataProperty)
            }
            safeDouble cell
        }
        builder
    }

    private HighDimProtos.ColumnValue.Builder columnValueBuilderSetupForString(
            HighDimProtos.ColumnValue.Builder builder, String dataProperty, DataRow<AssayColumn, ?> row) {
        builder.clear()
        builder.addAllStringValue tabularResult.indicesList.collect { AssayColumn col ->
            def cell = row.getAt col
            if (dataProperty) { // multi value projection
                cell = cell?.getAt(dataProperty)
            }
            safeString cell
        }
        builder
    }

    static void write(Projection projection,
                      TabularResult<AssayColumn, DataRow<AssayColumn,? extends Object>> tabularResult,
                      OutputStream out) {
        def highDimBuilder = new HighDimBuilder(projection, tabularResult)
        highDimBuilder.writeHeader out
        highDimBuilder.writeRows out
    }

    void writeHeader(OutputStream out) {
        createHeader().writeDelimitedTo(out)
    }

    void writeRows(OutputStream out) {
        while (rowsIterator.hasNext()) {
            createRow(rowsIterator.next()).writeDelimitedTo(out)
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
        headerBuilder.addAllAssay(tabularResult.indicesList
                .collect { AssayColumn col -> createAssay(assayBuilder, col) })

        headerBuilder.build()
    }

    public static ColumnType typeForClass(Class clazz) {
        Number.isAssignableFrom(clazz) ? ColumnType.DOUBLE : ColumnType.STRING
    }

    private SortedMap<String, Class> getDataProperties(Projection projection) {
        if (projection instanceof MultiValueProjection) {
            MultiValueProjection mvp = projection as MultiValueProjection
            Map<String, Class> dataProperties = Maps.transformValues(
                    mvp.dataProperties,
                    HighDimBuilder.&decideColumnValueType as Function)
            ImmutableSortedMap.copyOf dataProperties
        } else {
            // find the type of the first non-value of the first row
            ImmutableSortedMap.of 'value',
                    decideColumnValueType(
                            rowsIterator.peek().find().getClass())
        }
    }

    private static Class decideColumnValueType(Class originalClass) {
        if (originalClass in [double, float, long, int, short, byte] ||
                Number.isAssignableFrom(originalClass)) {
            return Double
        } else {
            // Anything that is not a number is serialized as string
            return String
        }
    }

    private Row createRow(DataRow<AssayColumn, ?> inputRow) {
        rowBuilder.clear()
        rowBuilder.label = inputRow.label
        if (inputRow instanceof BioMarkerDataRow) {
            rowBuilder.bioMarker = safeString(((BioMarkerDataRow) inputRow).bioMarker)
        }

        columnValueBuilderCreators.each {
                Closure<HighDimProtos.ColumnValue.Builder> builderClosure ->
            rowBuilder.addValue builderClosure.call(inputRow)
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

    private static String safeString(Object obj) {
        obj == null ? '' : obj.toString()
    }

    private static Double safeDouble(Object obj) {
        obj == null ? Double.NaN : obj as Double
    }
}
