package transmart.solr.indexing

interface FacetsField {
    String getFieldName()
    FacetsFieldType getType()
}
