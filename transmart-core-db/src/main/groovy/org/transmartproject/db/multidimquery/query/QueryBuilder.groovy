package org.transmartproject.db.multidimquery.query

interface QueryBuilder<ConstraintResult, QueryResult> {

    /**
     * Builds a queryable object for the {@link Constraint} object.
     *
     * @param constraint
     * @return the result.
     */
    ConstraintResult build(Constraint constraint)

    QueryResult buildCriteria(Constraint constraint)

    void build(Object obj)

}

