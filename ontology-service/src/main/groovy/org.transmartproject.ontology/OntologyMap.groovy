package org.transmartproject.ontology

/**
 * The ontology mapping used to map variables declared in the column mapping file to ontology codes
 * and to contain the ancestry of the classes those codes represent.
 * Contains information about concepts and the ontology tree that are uploaded to TranSMART.
 */
class OntologyMap {

    public static final String categoryCodeHeader = "Category code"
    public static final String dataLabelHeader = "Data label"
    public static final String ontologyCodeHeader = "Ontology code"
    public static final String labelHeader = "Label"
    public static final String uriHeader = "URI"
    public static final String ancestorsHeader = "Ancestors"

    /**
     * The category name used in the column mapping file.
     */
    String categoryCode

    /**
     * The variable name used in the column mapping file.
     */
    String dataLabel

    /**
     * Concept code to be used to classify observations.
     */
    String ontologyCode

    /**
     * Name of the concept.
     */
    String label

    /**
     * Link to resource with metadata about the concept.
     */
    String uri

    /**
     * Comma-separated list of ontology codes of classes of which this concept is a subclass.
     */
    List<String> ancestors

}
