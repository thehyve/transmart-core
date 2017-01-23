package annotation

class AmData {

    Long id
    String uniqueId
    String amDataType

    static mapping = {
        table 'AM_DATA_UID'
        version false
        id column: 'AM_DATA_ID', generator: 'assigned'
    }

}