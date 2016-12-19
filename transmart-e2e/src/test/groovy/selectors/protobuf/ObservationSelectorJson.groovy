package selectors.protobuf

import grails.converters.JSON

class ObservationSelectorJson {
    final ObservationsMessageJson hyperCubeMessage
    final int cellCount
    List<String> notInlined = []
    List<String> inlined = []

    ObservationSelectorJson(ObservationsMessageJson hyperCubeMessage) {
        this.hyperCubeMessage = hyperCubeMessage
        this.cellCount = hyperCubeMessage.cells.size()
        hyperCubeMessage.header.'dimensionDeclarations'.each
                {it -> if(it.inline){
                    inlined.add(it.name)
                } else {
                    notInlined.add(it.name)
                }}
    }

    def select(cellIndex, dimension, fieldName, valueType) {
        def result
        if (inlined.contains(dimension)){
            def index = inlined.indexOf(dimension)
            def dimObject = hyperCubeMessage.cells[cellIndex].inlineDimensions[index]
            result = fieldName ? dimObject[fieldName] : dimObject
        } else {
            // non inline
            def index = notInlined.indexOf(dimension)
            def valueIndex = hyperCubeMessage.cells[cellIndex].dimensionIndexes[index]
            def dimElements = hyperCubeMessage.footer.dimensions[index]
            result = fieldName ? dimElements[fieldName][valueIndex] : dimElements[valueIndex]
        }

        switch (valueType){
            case 'String':
                return result as String
            case 'Int':
                return result as int
            case 'Double':
                return result as double
            case 'Date':
                return result ? Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", result) : null
            default:
                return result
        }
    }

    def select(cellIndex, dimension){
        def index = notInlined.indexOf(dimension)
        def valueIndex = hyperCubeMessage.cells[cellIndex].dimensionIndexes[index]
        // return the object at position valueIndex of inlined dimension index.
        return hyperCubeMessage.footer.dimensions[index][valueIndex]
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
