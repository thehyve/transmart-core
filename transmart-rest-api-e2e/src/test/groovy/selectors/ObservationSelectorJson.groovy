package selectors

class ObservationSelectorJson {
    final ObservationsMessageJson hyperCubeMessage
    final int cellCount
    List<String> notInlined = []
    List<String> inlined = []

    ObservationSelectorJson(ObservationsMessageJson hyperCubeMessage) {
        this.hyperCubeMessage = hyperCubeMessage
        this.cellCount = hyperCubeMessage.cells.size()
        hyperCubeMessage.dimensionDeclarations.each {
            if(it.inline){
                inlined.add(it.name)
            } else {
                notInlined.add(it.name)
            }
        }
    }

    def select(cellIndex, dimension, fieldName, valueType) {
        def result
        int index = inlined.indexOf(dimension)
        if (index != -1){
            def dimObject = hyperCubeMessage.cells[cellIndex].inlineDimensions[index]
            result = fieldName ? dimObject?.getAt(fieldName) : dimObject
        } else {
            // non inline
            index = notInlined.indexOf(dimension)
            def valueIndex = hyperCubeMessage.cells[cellIndex].dimensionIndexes[index]
            if(valueIndex == null) return null
            def dimElement = hyperCubeMessage.dimensionElements[dimension]?.getAt(valueIndex)
            result = fieldName ? dimElement?.getAt(fieldName) : dimElement
        }

        switch (valueType){
            case 'String':
                return result as String
            case 'Int':
                return result as Integer
            case 'Double':
                return result as Double
            case 'Date':
                return result ? Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", result) : null
            default:
                return result
        }
    }

    def select(cellIndex, dimension){
        int index = notInlined.indexOf(dimension)
        if(index != -1) {
            def valueIndex = hyperCubeMessage.cells[cellIndex].dimensionIndexes[index]
            // return the object at position valueIndex of inlined dimension index.
            return hyperCubeMessage.dimensionElements[index][valueIndex]
        } else {
            index = inlined.indexOf(dimension)
            assert index != -1, "Dimension $dimension not found"
            return hyperCubeMessage.cells[cellIndex].inlineDimensions[index]
        }
    }

    /**
     * returns the value of a cell
     *
     * @param cellIndex
     * @return
     */
    def select(cellIndex){
        String stringValue = hyperCubeMessage.cells[cellIndex].stringValue
        return  stringValue == null ? hyperCubeMessage.cells[cellIndex].numericValue : stringValue
    }
}
