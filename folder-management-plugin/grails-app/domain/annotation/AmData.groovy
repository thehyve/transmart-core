package annotation

class AmData {

    Long id
    String uniqueId
    String amDataType

    static mapping = {
        table schema: 'amapp', name: 'am_data_uid'
        version false
        id column: 'AM_DATA_ID', generator: 'assigned'
    }

}
