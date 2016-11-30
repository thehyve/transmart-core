package selectors.protobuf

class ObservationSelectorJson {
    final ObservationsMessageJson hyperCubeMessage
    final int cellCount
    List<String> notInlined = []
    List<String> inlined = []

    ObservationSelectorJson(ObservationsMessageJson hyperCubeMessage) {
        this.hyperCubeMessage = hyperCubeMessage
        this.cellCount = hyperCubeMessage.cells.size()
        hyperCubeMessage.header.'dimensionDeclarations'.each
                {it -> if(it.inline){ //FIXME: this is a temporary fix for: TMPDEV-124 protobuf sterilization, meaning of inline:true is false
                    inlined.add(it.name)
                } else {
                    notInlined.add(it.name)
                }}
    }

    def select(cellIndex, dimansion, fieldName, valueType) {
        int dimensionDeclarationIndex = -1
        if (notInlined.contains(dimansion)){
            dimensionDeclarationIndex = notInlined.indexOf(dimansion)
        } else if (inlined.contains(dimansion)) {
            dimensionDeclarationIndex = inlined.indexOf(dimansion)
        }
        assert dimensionDeclarationIndex != -1, 'dimansion could not be found in header'

        int fieldsIndex

        if (fieldName) {
            hyperCubeMessage.header.'dimensionDeclarations'.each { dilist ->
                if (dilist.name.equalsIgnoreCase(dimansion)) {
                    dilist.'fields'.eachWithIndex {
                        field, index ->
                            if (field.name.equalsIgnoreCase(fieldName)) {
                                fieldsIndex = index
                            }
                    }
                }
            }
        } else {
            fieldsIndex = 0
        }

        if (inlined.contains(dimansion)){
            return hyperCubeMessage.cells.get(cellIndex).inlineDimensions[dimensionDeclarationIndex].fields[fieldsIndex]."${valueType.toLowerCase()}Value".val
        }

        //nonInline
        int dimensionIndexes = hyperCubeMessage.cells[cellIndex].dimensionIndexes[dimensionDeclarationIndex] as int
        def result = hyperCubeMessage.footer.dimension[dimensionDeclarationIndex].fields[fieldsIndex]."${valueType.toLowerCase()}Value"[dimensionIndexes].'val'

        switch (valueType){
            case 'String':
                return result as String
            case 'Int':
                return result as int
            case 'Double':
                return result as double
            case 'Timestamp':
                return result as Date
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
