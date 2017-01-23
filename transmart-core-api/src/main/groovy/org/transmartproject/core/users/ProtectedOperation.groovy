package org.transmartproject.core.users

interface ProtectedOperation {

    enum WellKnownOperations implements ProtectedOperation {
        /**
         * Make use of the read of operations of the REST API.
         * Applicable to studies.
         */
        API_READ,

        /**
         * Create a cohort that includes the protected resource.
         * Applicable to studies and query definitions.
         */
        BUILD_COHORT,

        /**
         * Show summary statistics of the resource (histogram of the
         * distribution and demographic information).
         * Applicable to studies and query results.
         */
        SHOW_SUMMARY_STATISTICS,

        /**
         * Show the protected resource in the grid view.
         * Applicable to studies and query results.
         */
        SHOW_IN_TABLE,

        /**
         * Export any part of the protected resource.
         * Applicable to studies and query results.
         */
        EXPORT,

        /**
         * Run an analysis including data from the protected resource
         * Applicable to studies and query results.
         */
        RUN_ANALYSIS,

        /**
         * Read the protected resource.
         * Applicable to query results.
         */
        READ,
    }
}
