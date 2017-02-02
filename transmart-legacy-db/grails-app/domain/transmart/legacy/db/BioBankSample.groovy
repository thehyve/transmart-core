package transmart.legacy.db

class BioBankSample {
    String id
    String client_sample_tube_id
    String container_id
    String source_type
    String accession_number
    Date import_date

    static mapping = {
        table 'BIOMART.BIOBANK_SAMPLE'
        version false
        id column: 'SAMPLE_TUBE_ID'
        client_sample_tube_id column: 'CLIENT_SAMPLE_TUBE_ID'
        container_id column: 'CONTAINER_ID'
        source_type column: 'SOURCE_TYPE'
        accession_number column: 'ACCESSION_NUMBER'
        import_date column: 'IMPORT_DATE'
    }

}
