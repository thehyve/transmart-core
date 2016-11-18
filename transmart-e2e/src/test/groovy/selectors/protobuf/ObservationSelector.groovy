package selectors.protobuf

import protobuf.ObservationsMessageProto
import protobuf.ObservationsProto

class ObservationSelector {

    final ObservationsMessageProto protoMessage
    List<String> notInlined = []
    List<String> inlined = []

    ObservationSelector(ObservationsMessageProto protoMessage) {
        this.protoMessage = protoMessage
        protoMessage.header.dimensionDeclarationsList.each
                {it -> if(!it.inline){ //FIXME: this is a teperary fix for: TMPDEV-124 protobuf sterilization, meaning of inline:true is false
                    inlined.add(it.name)
                } else {
                    notInlined.add(it.name)
                }}
    }

    def selectStringValue(cellIndex, dimansion, fieldName) {
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

        return protoMessage.footer.getDimension(dimensionDeclarationIndex).getFields(fieldsIndex).getStringValue(dimensionIndexes).'val'
    }

    def selectDoubleValue(cellIndex, dimansion, fieldName) {
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

        return protoMessage.footer.getDimension(dimensionDeclarationIndex).getFields(fieldsIndex).getStringValue(dimensionIndexes)
    }

    def selectIntValue(cellIndex, dimansion, fieldName) {
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

        return protoMessage.footer.getDimension(dimensionDeclarationIndex).getFields(fieldsIndex).getIntValue(dimensionIndexes)
    }

    def selectTimestampValue(cellIndex, dimansion, fieldName) {
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

        return protoMessage.footer.getDimension(dimensionDeclarationIndex).getFields(fieldsIndex).getTimestampValue(dimensionIndexes)
    }
}
