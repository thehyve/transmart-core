package org.transmartproject.core.dataquery.highdim.dataconstraints

/**
 * Identifies a data constraint, that is, a constraint that will limit the
 * rows returned in a result set.
 *
 * This is only a marker interface.
 *
 * Some constants that identify well-know, general constraints are included.
 * This interface should only include constants for constraints that are
 * expected to be support in a significant number of different analyses.
 */
public interface DataConstraint {

    /**
     * A well-known constraint for filtering rows related to a certain
     * search keyword. This includes genes, pathways and whatever else is
     * stored as search keywords.
     *
     * NOTE: This is actually a pretty unsatisfactory constraint because
     *       it leaks implementation details like a sieve. However, the
     *       frontend currently sends these ids, so we have to deal with it.
     *       A possibly better constraint would receive the actual keyword
     *       (e.g. gene name) and entity type (e.g. gene) and work with that
     *       instead of numeric ids.
     *
     * Parameters: 'keyword_ids' => <List of keyword ids (PK of search_keyword)>
     */
    public final static String SEARCH_KEYWORD_IDS_CONSTRAINT = 'search_keyword_ids'

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
