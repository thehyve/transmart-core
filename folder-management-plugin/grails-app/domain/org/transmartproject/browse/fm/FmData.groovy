package org.transmartproject.browse.fm

class FmData {

    Long id
    String uniqueId
    String fmDataType

    static mapping = {
        table schema: 'fmapp', name: 'fm_data_uid'
        version false
        id column: 'FM_DATA_ID', generator: 'assigned'
    }

}
