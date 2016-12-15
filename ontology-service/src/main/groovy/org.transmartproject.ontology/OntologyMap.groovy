package org.transmartproject.ontology

/**
 * Created by ewelina on 12-12-16.
 */
class OntologyMap {
    /**
     * The ontology mapping used to map variables declared in the column mapping file to ontology codes
     * and to contain the ancestry of the classes those codes represent.
     * Contains information about concepts and the ontology tree that are uploaded to TranSMART.
     * @param categoryCode
     * @param dataLabel
     * @param ontologyCode
     * @param label
     * @param uri
     * @param ancestors
     */
    OntologyMap(categoryCode, dataLabel, ontologyCode, label, uri, ancestors) {
        this.categoryCode = categoryCode
        this.dataLabel = dataLabel
        this.ontologyCode = ontologyCode
        this.label = label
        this.uri = uri
        this.ancestors = ancestors
    }

    OntologyMap(ontologyCode, label, uri, ancestors) {
        this.categoryCode = ""
        this.dataLabel = ""
        this.ontologyCode = ontologyCode
        this.label = label
        this.uri = uri
        this.ancestors = ancestors
    }

    public static final String categoryCodeHeader = "Category code"
    public static final String dataLabelHeader = "Data label"
    public static final String ontologyCodeHeader = "Ontology code"
    public static final String labelHeader = "Label"
    public static final String uriHeader = "URI"
    public static final String ancestorsHeader = "Ancestors"

    /**
     * The category name used in the column mapping file.
     */
    def categoryCode

    /**
     * The variable name used in the column mapping file.
     */
    def dataLabel

    /**
     * Concept code to be used to classify observations.
     */
    def ontologyCode

    /**
     * Name of the concept.
     */
    def label

    /**
     * Link to resource with metadata about the concept.
     */
    def uri

    /**
     * Comma-separated list of ontology codes of classes of which this concept is a subclass.
     */
    def ancestors

}
