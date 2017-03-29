package org.transmartproject.rest.serialization

import com.google.protobuf.ValueOrBuilder
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import org.transmartproject.rest.hypercubeProto.ObservationsProto.CellOrBuilder
import org.transmartproject.rest.hypercubeProto.ObservationsProto.DimensionElementOrBuilder

/**
 * Static helper methods to decode hypercube protobuf messages into a more idiomatic representation. These functions
 * can also be used in clients or inspire client implementations. These functions also do some checks on the validity
 * of the protobuf messages and will throw on invalid messages.
 * Note that these functions are written to be concise and simple to read, not to be fast.
 */
class DeserializationHelper {
    /**
     * Parse a list of items where absent values are indicated by a separate list of 1-based indices (the format used
     * in the protobuf serialization).
     * @param elements
     * @param absentIndices
     * @return the elements with absents represented as nulls
     */
    static <T> List<T> handleAbsents(List<T> elements, List<Integer> absentIndices) {
        // absentIndices is 1-based
        int elemIdx = 0
        int absentIdx = 0
        List result = []
        for(i in 0..<(elements.size() + absentIndices.size())) {
            if((absentIndices[absentIdx] ?: 0) - 1 == i) {
                result << null
                absentIdx++
            } else {
                result << elements.get(elemIdx++)
            }
        }
        return result
    }

    static def getValue(ObservationsProto.ValueOrBuilder value) {
        def cse = value.valueCase
        if(cse == ObservationsProto.Value.ValueCase.STRINGVALUE) return value.stringValue
        if(cse == ObservationsProto.Value.ValueCase.INTVALUE) return value.intValue
        if(cse == ObservationsProto.Value.ValueCase.DOUBLEVALUE) return value.doubleValue
        if(cse == ObservationsProto.Value.ValueCase.TIMESTAMPVALUE) return new Date(value.timestampValue)
        assert false // a value must be set
    }

    static def decodeDimensionElement(DimensionElementOrBuilder dimElement, Collection properties = null) {
        def value = [
                (ObservationsProto.DimensionElement.ValueCase.STRINGVALUE): dimElement.stringValue,
                (ObservationsProto.DimensionElement.ValueCase.INTVALUE): dimElement.intValue,
                (ObservationsProto.DimensionElement.ValueCase.DOUBLEVALUE): dimElement.doubleValue,
                (ObservationsProto.DimensionElement.ValueCase.TIMESTAMPVALUE): new Date(dimElement.timestampValue),
                (ObservationsProto.DimensionElement.ValueCase.VALUE_NOT_SET): dimElement.fieldsList,
        ][dimElement.valueCase]

        if(!(value instanceof List)) {
            assert dimElement.fieldsCount == 0
            assert dimElement.absentFieldIndicesCount == 0
            return value
        }

        if(value == []) return null

        value = handleAbsents(value.collect { getValue(it) }, dimElement.absentFieldIndicesList)

        if(properties != null) {
            assert value.size() == properties.size()
            return [properties*.name, value].transpose().collectEntries()
        }

        return value
    }

    /**
     * Decode a DimensionElementFieldColumn into a list of values (the column)
     * @param column
     * @param type
     * @param additionalAbsents
     * @return
     */
    static List parseFieldColumn(ObservationsProto.DimensionElementFieldColumnOrBuilder column, Class type,
                                 List<Integer> additionalAbsents = []) {
        def values = [
                (String): column.stringValueList,
                (Double): column.doubleValueList,
                (Integer): column.intValueList,
                (Long): column.intValueList,
                (Date): column.timestampValueList,
        ][type]
        if(type != String) assert column.stringValueCount == 0
        if(type != Double) assert column.doubleValueCount == 0
        if(type != Date) assert column.timestampValueCount == 0
        if(type != Integer && type != Long) assert column.intValueCount == 0

        def absents = column.absentValueIndicesList
        if(additionalAbsents) {
            absents += additionalAbsents
            absents.sort()
        }
        return handleAbsents(values, absents)
    }

    /**
     * Parses a DimensionElements into a list of properties
     *
     * @param dimElems a protobuf DimensionElements object (or builder)
     * @param propertiesOrType A collection of {@Link org.transmartproject.core.multidimquery.Property}s, or of objects
     * that have a 'name': String and 'type': Class property. Alternatively, a single type. In that case
     * DimensionElements must have a single column, the values of which are returned as a list.
     * @return A list of elements, with each element as a map of propertyName: value pairs. If the DimensionElements
     * is empty we don't know how many elements it represents, so this returns null. If `properties` is not specified
     * the return value is a list of plain elements.
     */
    static List parseDimensionElements(ObservationsProto.DimensionElementsOrBuilder dimElems, propertiesOrType) {
        List<List> columns = []
        if(dimElems.empty) return null
        def columnDefs = handleAbsents(dimElems.fieldsList, dimElems.absentFieldColumnIndicesList)
        if(propertiesOrType instanceof Class) {
            assert columnDefs.size() == 1
            def columnDef = columnDefs[0]
            assert columnDef != null, "All columns null, the `empty` flag should have been set"
            return parseFieldColumn(columnDef, propertiesOrType, dimElems.absentElementIndicesList)
        }
        def properties = propertiesOrType as List

        assert columnDefs.size() == properties.size()
        for(c_prop in [columnDefs, properties].transpose()) {
            ObservationsProto.DimensionElementFieldColumnOrBuilder c = c_prop[0]
            Class type = c_prop[1].type
            if(c == null) {
                columns << null
                continue
            }
            columns << parseFieldColumn(c, type, dimElems.absentElementIndicesList)
        }
        def sizes = columns.findAll { it != null }.collect { it.size() }
        assert sizes.min() == sizes.max()
        def size = sizes[0]

        columns = columns.collect { it == null ? [null] * size : it }

        List<String> propNames = properties*.name
        def fields = columns.transpose()
        return fields.collect { [propNames, it].transpose().collectEntries() }
    }


    static def cellValue(ObservationsProto.CellOrBuilder cell) {
        if(cell.valueCase == ObservationsProto.Cell.ValueCase.VALUE_NOT_SET) return null
        if(cell.valueCase == ObservationsProto.Cell.ValueCase.STRINGVALUE) return cell.stringValue
        if(cell.valueCase == ObservationsProto.Cell.ValueCase.NUMERICVALUE) return cell.numericValue
        assert false
    }

    static def decodeIndexedDim(Integer idx, List elements) {
        idx == 0 ? null : elements[idx-1 as Integer]
    }

    static List inlineDims(CellOrBuilder cell, List<Dimension> dimensions = null) {
        def elems = handleAbsents(cell.inlineDimensionsList, cell.absentInlineDimensionsList)
        if(dimensions == null) {
            return elems.collect { it == null ? null : decodeDimensionElement(it)}
        }
        [elems, dimensions].transpose().collect { elem, Dimension dim ->
            elem ? decodeDimensionElement(elem, dim?.elementFields?.values()) : null
        }
    }

    static List indexedDims(CellOrBuilder cell, Collection<List> indexedDms) {
        def indexes = cell.dimensionIndexesList.collect { it == 0l ? null : it-1 as Integer }
        [indexes, indexedDms as List].transpose().collect { Integer idx, List elems -> idx != null ? elems[idx] : null }
    }

    static Map decodeCell(ObservationsProto.CellOrBuilder cell, Map<Dimension, List> indexedDms, List<Dimension> inlineDms) {
        def elem = [inlineDms*.name, inlineDims(cell, inlineDms)].transpose().collectEntries()
        elem += [indexedDms.keySet()*.name, indexedDims(cell, indexedDms.values())].transpose().collectEntries()
        elem.value = cellValue(cell)
        elem
    }

}
