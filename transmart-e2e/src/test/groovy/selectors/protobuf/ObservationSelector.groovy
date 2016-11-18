package selectors.protobuf

import protobuf.ObservationsMessageProto
import protobuf.ObservationsProto

class ObservationSelector {

    final ObservationsMessageProto protoMessage
    final int cellCount
    List<String> notInlined = []
    List<String> inlined = []

    ObservationSelector(ObservationsMessageProto protoMessage) {
        this.protoMessage = protoMessage
        this.cellCount = protoMessage.cells.size()
        protoMessage.header.dimensionDeclarationsList.each
                {it -> if(!it.inline){ //FIXME: this is a temporary fix for: TMPDEV-124 protobuf sterilization, meaning of inline:true is false
                    inlined.add(it.name)
                } else {
                    notInlined.add(it.name)
                }}
    }

    /**
     * returns a value of a specific field
     *
     * @param cellIndex
     * @param dimansion
     * @param fieldName
     * @param valueType
     * @return
     */
    def select(cellIndex, dimansion, fieldName, valueType) {
        int dimensionDeclarationIndex = notInlined.indexOf(dimansion)

        int fieldsIndex

        protoMessage.header.dimensionDeclarationsList.each { dilist ->
            if (dilist.name.equalsIgnoreCase(dimansion)) {
                dilist.fieldsList.eachWithIndex {
                    field, index ->
                        if (field.name.equalsIgnoreCase(fieldName)) {
                            fieldsIndex = index
                        }
                }
            }
        }

        int dimensionIndexes = protoMessage.cells.get(cellIndex).getDimensionIndexes(dimensionDeclarationIndex)

        return protoMessage.footer.getDimension(dimensionDeclarationIndex).getFields(fieldsIndex).invokeMethod("get${valueType}Value",dimensionIndexes).'val'
    }

    /**
     * returns the value of a cell
     *
     * @param cellIndex
     * @return
     */
    def select(cellIndex){
        String stringValue = protoMessage.cells.get(cellIndex).getStringValue()
        return  stringValue.empty ? protoMessage.cells.get(cellIndex).getNumericValue() : stringValue
    }
}
