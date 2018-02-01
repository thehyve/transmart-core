package org.transmartproject.core.tree

import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermType

/**
 * The ontology node class that represents nodes in the ontology forest.
 * (There may be multiple root nodes, hence the name 'forest'.)
 * Intermediate nodes contain a collection of child nodes.
 * Leaf nodes represent a concept (either variable or value) or a study in TranSMART.
 */
interface TreeNode {

    /**
     * API version string used for marshalling the object.
     * @return the API version string.
     */
    String getApiVersion()

    void setApiVersion(String version)

    /**
     * Retrieves the parent node if it exists (i.e., when it is not a root node).
     * @return the parent node.
     */
    TreeNode getParent()

    /**
     * Retrieves the child nodes.
     * @return the child nodes.
     */
    List<TreeNode> getChildren()

    /**
     * Retrieves the tags associated with the node.
     * @return the list of tags.
     */
    List<OntologyTermTag> getTags()

    /**
     * The number of observations that satisfy the constraint in {@link #getConstraint()}.
     * The count is not always set, it can also be null.
     * @return the number of observations for this node if set; null otherwise.
     */
    Long getObservationCount()

    /**
     * The number of patients that have observations that satisfy the constraint in {@link #getConstraint()}.
     * The count is not always set, it can also be null.
     * @return the number of patients for this node if set; null otherwise.
     */
    Long getPatientCount()

    /**
     * The name of the node.
     * @return the name.
     */
    String getName()

    /**
     * The full tree path that contains all ancestors from the root up to the current node and the name of the node.
     * @return the path.
     */
    String getFullName()

    /**
     * The visual attributes that characterise how the node should be represented.
     * @return the set of visual attributes.
     */
    EnumSet<OntologyTerm.VisualAttributes> getVisualAttributes()

    /**
     * The dimension that the current node refers to. Could be study or concept.
     */
    String getDimension()

    /**
     * The concept code of the concept that this node represents, if it represents a concept; null otherwise.
     */
    String getConceptCode()

    /**
     * The concept path of the concept that this node represents, if it represents a concept; null otherwise.
     */
    String getConceptPath()

    /**
     * Returns the node itself if itself is a study node or an ancestry study node if it exists.
     */
    TreeNode getStudy()

    /**
     * Retrieves the dimension code as study id if the node is a study node,
     * the table name is 'study' and the column name is 'study_id';
     * null otherwise.
     */
    String getStudyId()

    /**
     * The type of the node, either a container type or a variable type.
     * @return the type.
     */
    OntologyTermType getOntologyTermType()

    /**
     * Create a constraint object for the query that this node represents.
     *
     * Supports study, concept, modifier, patient and trial visit nodes.
     *
     * @return the constraint map for this node.
     */
    MultiDimConstraint getConstraint()

}
