package org.transmartproject.core.dataquery.highdim.assayconstraints

import org.transmartproject.core.dataquery.highdim.HighDimensionResource

/**
 * Marker interface for limiting the assays returned in the the result of a
 * high dimensional data query.
 */
public interface AssayConstraint {

    /**
     * A well-known constraint for filtering assays pertaining to a specific
     * trial name (study name).
     *
     * Parameters: 'name' => <trial name>
     */
    public final static String TRIAL_NAME_CONSTRAINT = 'trial_name'

    /**
     * A well-known constraint for limiting the assays that are linked to
     * patients in a given patient set.
     *
     * Parameters: 'result_instance_id' => <result instance id>
     */
    public final static String PATIENT_SET_CONSTRAINT = 'patient_set'

    /**
     * Assays are associated with a specific node. This well-known constraint
     * limits the assays to the terms associated with the given node.
     *
     * NOTE: child nodes are NOT included. The parameter should be a leaf term
     *       that is directly associated with the assays.
     *
     * Parameters: 'concept_key' => <full concept path, table code included>
     */
    public final static String ONTOLOGY_TERM_CONSTRAINT = 'ontology_term'

    public final static String CONCEPT_PATH_CONSTRAINT = 'concept_path'

    /**
     * Well-known assay constraint for filtering the assays by id. Useful to
     * pair with {@link HighDimensionResource#getSubResourcesAssayMultiMap(List)}.
     *
     * Parameters: 'ids' => <list of numeric ids>
     */
    public final static String ASSAY_ID_LIST_CONSTRAINT = 'assay_id_list'

    /**
     * Well-known assay constraint for filtering the assays by patient id. The
     * patient id is the external id, also known as in-trial id.
     *
     * Parameters: 'ids' => <list of String ids>
     */
    public final static String PATIENT_ID_LIST_CONSTRAINT = 'patient_id_list'

    /**
     * A well-known constraint for building a disjunction of several
     * sub-constraints.
     *
     * Parameters: 'subconstraints' => <map with string key (the constraint
     *                                  name) and a map value (the parameters of
     *                                  the sub-constraint) or a list of maps
     *                                  value (for several constraints of the
     *                                  same type)>
     */
    public final static String DISJUNCTION_CONSTRAINT = 'disjunction'
}
