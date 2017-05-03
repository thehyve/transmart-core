package org.transmartproject.rest.serialization

import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import org.transmartproject.rest.hypercubeProto.ObservationsProto.CellOrBuilder
import org.transmartproject.rest.hypercubeProto.ObservationsProto.DimensionElementOrBuilder
import org.transmartproject.rest.hypercubeProto.ObservationsProto.DimensionElements.ScopeCase

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
     * @param defaultValue (default null) the value to use for absents
     * @return the elements with absents represented as nulls (or defaultValue's)
     */
    static <T> List<T> handleAbsents(List<T> elements, List<Integer> absentIndices, defaultValue=null) {
        // absentIndices is 1-based
        int elemIdx = 0
        int absentIdx = 0
        List result = []
        for(i in 0..<(elements.size() + absentIndices.size())) {
            if((absentIndices[absentIdx] ?: 0) - 1 == i) {
                result << defaultValue
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

    static def decodeDimensionElement(DimensionElementOrBuilder dimElement, Collection<String> propertyNames = null) {
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

        if(propertyNames != null) {
            assert value.size() == propertyNames.size()
            return [propertyNames.asList(), value].transpose().collectEntries()
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
    static List parseFieldColumn(ObservationsProto.DimensionElementFieldColumnOrBuilder column,
                                 List<Integer> additionalAbsents = []) {
        def values = column.stringValueList ?: column.doubleValueList ?: column.intValueList ?:
                column.timestampValueList.collect { new Date(it) }

        assert [column.stringValueCount, column.intValueCount, column.doubleValueCount, column.timestampValueCount]
                .collect { it == 0 ? 0 : 1 }.sum() == 1

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
     * @param propertyNames A collection of Strings representing the property names that each element has. Must be
     * set for elements that are composite, must not be set for elements that are not composite.
     * @return A list of elements, with each element as a map of propertyName: value pairs for composite elements
     * (`propertyNames` must be set), or just a list of elements for non-composite serializable elements (in which
     * case `propertyNames` must not be set).
     */
    static List parseDimensionElements(ObservationsProto.DimensionElementsOrBuilder dimElems, Collection<String> propertyNames=null) {
        List<List> columns = []
        if(dimElems.empty) return null
        def columnDefs = handleAbsents(dimElems.fieldsList, dimElems.absentFieldColumnIndicesList)
        if(propertyNames == null) {
            assert columnDefs.size() == 1
            def columnDef = columnDefs[0]
            assert columnDef != null, "All columns null, the `empty` flag should have been set"
            return parseFieldColumn(columnDef, dimElems.absentElementIndicesList)
        }

        assert columnDefs.size() == propertyNames.size()
        for(c in columnDefs) {
            if(c == null) {
                columns << null
                continue
            }
            columns << parseFieldColumn(c, dimElems.absentElementIndicesList)
        }
        def sizes = columns.findAll { it != null }.collect { it.size() }
        assert sizes.min() == sizes.max()
        def size = sizes[0]

        columns = columns.collect { it == null ? [null] * size : it }

        def fields = columns.transpose()
        return fields.collect { [propertyNames.asList(), it].transpose().collectEntries() }
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
            elem ? decodeDimensionElement(elem, dim?.elementFields?.keySet()) : null
        }
    }

    static List indexedDims(/*PackedCellOrCellOrBuilder*/ cell, Collection<List> indexedDms) {
        def indexes = cell.dimensionIndexesList.collect { it == 0l ? null : it-1 as Integer }
        [indexes, indexedDms as List].transpose().collect { Integer idx, List elems -> idx != null ? elems[idx] : null }
    }

    static Map decodeCell(CellOrBuilder cell, Map<Dimension, List> indexedDms, List<Dimension> inlineDms) {
        def elem = [inlineDms*.name, inlineDims(cell, inlineDms)].transpose().collectEntries()
        elem += [indexedDms.keySet()*.name, indexedDims(cell, indexedDms.values())].transpose().collectEntries()
        elem.value = cellValue(cell)
        elem
    }

    // This flag indicates which encodings of inline dimensions have been seen, to ensure that all code paths have
    // been properly tested.
    public static int inlineDimEncodingsSeen = 0
    static List<Map> decodePackedCell(ObservationsProto.PackedCellOrBuilder packedCell, Dimension packedDim,
                                      Map<Dimension,List> indexedDms, List<Dimension> inlineDms) {

        def indexedDims = [indexedDms.keySet()*.name, indexedDims(packedCell, indexedDms.values())].transpose().collectEntries()

        def values = handleAbsents(packedCell.numericValuesList ?: packedCell.stringValuesList, packedCell.nullValueIndicesList)
        def nullElementValues = values[0..<packedCell.nullElementCount]
        def nonNullElementValues = values[packedCell.nullElementCount..<values.size()]
        List<List> groupedValues = [nullElementValues]
        if(packedCell.sampleCountsCount > 0) {
            def iter = nonNullElementValues.iterator()
            packedCell.sampleCountsList.each {
                groupedValues << getItems(iter, it)
            }
            assert !iter.hasNext()
        } else {
            groupedValues += handleAbsents(nonNullElementValues.collect { [it] }, packedCell.absentValuesList, [].asImmutable())
        }

        // values as map and set indexed dimensions
        List<List<Map>> valueMaps = groupedValues.collectNested { [value: it] + indexedDims }

        // Set packed dimension
        [[null] + indexedDms[packedDim], valueMaps].transpose().each { packElem, List<Map> vals ->
            vals.each { it[packedDim.name] = packElem }
        }

        // set inline dimensions
        assert inlineDms.size() == packedCell.inlineDimensionsCount
        for(i in inlineDms.indices) {
            Dimension dim = inlineDms[i]
            def propertyNames = dim.elementsSerializable ? [dim.name] : dim.elementFields.keySet()

            def dimElementsProto = packedCell.inlineDimensionsList[i]
            List<Map> dimElements = parseDimensionElements(dimElementsProto, propertyNames)

            if(dimElementsProto.perPackedCell) {
                assert dimElements.size() == 1
                for(l in valueMaps) for(m in l) {
                    m.putAll dimElements[0]
                }
                inlineDimEncodingsSeen |= 1
            } else if(dimElementsProto.scopeCase == ScopeCase.SCOPE_NOT_SET) {
                def nonEmptyMaps = valueMaps.findAll()
                assert dimElements.size() == nonEmptyMaps.size()

                [dimElements, nonEmptyMaps].transpose().each { Map elem, List<Map> vals ->
                    for(m in vals) m.putAll elem
                }
                inlineDimEncodingsSeen |= 2
            } else if(dimElementsProto.perSample) {
                assert dimElements.size() == valueMaps.sum { it.size() }

                [dimElements, valueMaps.flatten()].transpose().each { Map elem, Map value ->
                    value.putAll elem
                }
                inlineDimEncodingsSeen |= 4
            } else assert false, "invalid scope"
        }

        (List<Map>) valueMaps.flatten()
    }

    static final List getItems(Iterator iter, int count) {
        def res = []
        while(count--) {
            res << iter.next()
        }
        res
    }

}
