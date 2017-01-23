package fm

class FmData {

    Long id
    String uniqueId
    String fmDataType

    static mapping = {
        table 'FM_DATA_UID'
        version false
        id column: 'FM_DATA_ID', generator: 'assigned'
    }

}