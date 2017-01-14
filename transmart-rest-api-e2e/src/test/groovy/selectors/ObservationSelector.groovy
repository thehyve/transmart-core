package selectors

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.*

class ObservationSelector {

    final ObservationsMessageProto protoMessage
    final int cellCount
    List<String> notInlined = []
    List<String> inlined = []

    ObservationSelector(ObservationsMessageProto protoMessage) {
        this.protoMessage = protoMessage
        this.cellCount = protoMessage.cells.size()
        protoMessage.header.dimensionDeclarationsList.each {
            if(it.inline){
                inlined.add(it.name)
            } else {
                notInlined.add(it.name)
            }
        }
    }

    final static Set<String> validValueTypes = (["Double", "String", "Int", "Timestamp"] as HashSet).asImmutable()

    /**
     * returns a value of a specific field
     *
     * @param cellIndex
     * @param dimension
     * @param fieldName
     * @param valueType one of "Double", "String", "Int", "Timestamp"
     * @return
     */
    def select(int cellIndex, String dimension, String fieldName, String valueType) {
        assert valueType in validValueTypes, "Invalid type: $valueType not in $validValueTypes"

        int dimensionDeclarationIndex = -1
        if (notInlined.contains(dimension)){
            dimensionDeclarationIndex = notInlined.indexOf(dimension)
        } else if (inlined.contains(dimension)) {
            dimensionDeclarationIndex = inlined.indexOf(dimension)
        }
        assert dimensionDeclarationIndex != -1, "dimension $dimension could not be found in header"

        int fieldsIndex
        if (fieldName != null) {
            protoMessage.header.dimensionDeclarationsList.each { dilist ->
                if (dilist.name.equalsIgnoreCase(dimension)) {
                    dilist.fieldsList.eachWithIndex { field, index ->
                        if (field.name.equalsIgnoreCase(fieldName)) {
                            fieldsIndex = index
                        }
                    }
                }
            }
            assert fieldsIndex != null, "field $fieldName not present on dimension $dimension"
        } else {
            fieldsIndex = 0
        }

        Cell cell = protoMessage.cells.get(cellIndex)

        if (inlined.contains(dimension)) {
            DimensionElement element = retrieveNullable(cell.inlineDimensionsList, dimensionDeclarationIndex, cell.absentInlineDimensionsList)
            def valueholder
            if(fieldsIndex) {
                valueholder = retrieveNullable(element.fieldsList, fieldsIndex, element.absentFieldIndicesList)
            } else {
                valueholder = element
            }
            return valueholder?.invokeMethod("get${valueType}Value", null)
        }

        //nonInline
        long dimIndexL = cell.getDimensionIndexes(dimensionDeclarationIndex)
        if(dimIndexL == 0L) return null
        dimIndexL-- // convert from 1-based to 0-based
        assert dimIndexL <= Integer.MAX_VALUE, "Cell.dimensionIndexes value $dimIndexL cannot be converted to int, value too large"
        int dimIndex = (int) dimIndexL

        DimensionElements elements = protoMessage.footer.getDimension(dimensionDeclarationIndex)

        DimensionElementFieldColumn fieldColumn
        if(fieldsIndex) {
            fieldColumn = retrieveNullable(elements.fieldsList, fieldsIndex, elements.absentFieldColumnIndicesList)
        } else {
            fieldColumn = elements.absentFieldColumnIndicesList == [1] ? null : elements.getFields(0)
        }

        return retrieveNullable(fieldColumn?.invokeMethod("get${valueType}ValueList", null), dimIndex, fieldColumn.absentValueIndicesList)
    }

    /**
     * Lists of values that may be null are encoded in the protobuf format in several instances as a list of values
     * and a list of null indices. The null indices list is 1-based, and the list of values simply skips nulls.
     * @return the value or null
     */
    private def retrieveNullable(List values, int index, List<Integer> absentIndices) {
        if(values == null) return null
        assert values.size() + absentIndices.size() > index, "invalid value retrieval"

        // The number of values not present before the one we are looking for
        int skippedAbsentees = 0
        for(i in absentIndices) {
            // this list is 1-based
            if(i-1 < index) skippedAbsentees++
            else if(i-1 == index) return null
            else break
        }
        return values[index-skippedAbsentees]
    }

    def select(int cellIndex, String dimension, String valueType){
        return select(cellIndex, dimension, null, valueType)
    }

    /**
     * returns the value of a cell
     *
     * @param cellIndex
     * @return the value of a cell
     */
    def select(cellIndex){
        String stringValue = protoMessage.cells.get(cellIndex).stringValue
        return  stringValue.empty ? protoMessage.cells.get(cellIndex).numericValue : stringValue
    }
}
