package org.transmartproject.rest.protobug

import org.transmartproject.rest.hypercubeProto.ObservationsProto

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.Type.*

class ProtobufParser {

    static parseDimension(ObservationsProto.Header header, ObservationsProto.Footer footer) {
        def dimensions = header.dimensionDeclarationsList.collectEntries { [it.name, it] }
        def elements = header.dimensionDeclarationsList.collectEntries { decl ->
            def elems = decl.elements
            if (elems == null) {
                elems = footer.dimensionList.find {it.name == decl.name }
                assert elems != null
            }
            [decl.name, elems]
        }




    }

    static List getColumn(ObservationsProto.DimensionElementFieldColumn column) {
        String type
        def types = [STRING, INT, DOUBLE, TIMESTAMP, OBJECT]*.name()*.toLowerCase() + ['unpacked']
        types.each {
            if(column."${it}ValueCount" != 0) {
                if(type != null) {
                    throw new IllegalStateException("Only one type of field column can be set in $column")
                }
                type = it
            }
        }

        def ret
        if(type != 'object' && type != 'unpacked') {
            ret = column."${type}ValueList"
            return absentise(ret, column.absentValueIndicesList)
        }

        if(type == 'unpacked') {
            //TODO
        }

        if(type == 'object') {
            def columnMap = column.objectValueList.collectEntries { [it.key, getColumn(it.value)] }
            if ((columnMap.values()*.size() as Set).size() > 1) {
                throw new IllegalStateException("All columns in a map must be of equal size")
                // or do we omit trailing absentees?
            }

            def maps = (0..(columnMap.values()*.size().max())).collect { index ->
                columnMap.collectEntries { name, col ->
                    [name, col[index]]
                }
            }
            return maps
        }
    }

    static List absentise(List items, List<Integer> absentIndices) {
        def result = []
        int index = 0
        int absentPtr = 0
        int itemsPtr = 0
        while(itemsPtr < items.size() && absentPtr < absentIndices.size()) {
            if(absentIndices[absentPtr] == index) {
                result.add(null)
                absentPtr++
                index++
            } else {
                result.add(items[itemsPtr++])
                index++
            }
        }
        result
    }


}
