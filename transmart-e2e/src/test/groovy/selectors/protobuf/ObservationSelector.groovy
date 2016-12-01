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
                {it -> if(it.inline){
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
     * @param valueType Double, String, Int, Timestamp
     * @return
     */
    def select(cellIndex, dimansion, fieldName, valueType) {
        int dimensionDeclarationIndex = -1
        if (notInlined.contains(dimansion)){
            dimensionDeclarationIndex = notInlined.indexOf(dimansion)
        } else if (inlined.contains(dimansion)) {
            dimensionDeclarationIndex = inlined.indexOf(dimansion)
        }
        assert dimensionDeclarationIndex != -1, 'dimansion could not be found in header'

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

        if (inlined.contains(dimansion)){
            return protoMessage.cells.get(cellIndex).getInlineDimensions(dimensionDeclarationIndex).getFields(fieldsIndex).invokeMethod("get${valueType}Value", null).val
        }

        //nonInline
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
