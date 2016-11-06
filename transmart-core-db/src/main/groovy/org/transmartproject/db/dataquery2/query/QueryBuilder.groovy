package org.transmartproject.db.dataquery2.query

interface QueryBuilder<ConstraintResult, QueryResult> {

    /**
     * Builds a queryable object for the {@link Constraint} object.
     *
     * @param constraint
     * @return the result.
     */
    ConstraintResult build(Constraint constraint)

    /**
     * Builds a queryable object for the {@link Query} object.
     *
     * @param query
     * @return the result.
     */
    QueryResult build(Query query)

    void build(Object obj)

}

