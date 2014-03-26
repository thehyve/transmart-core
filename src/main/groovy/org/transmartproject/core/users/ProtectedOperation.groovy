package org.transmartproject.core.users

interface ProtectedOperation {

    enum WellKnownOperations implements ProtectedOperation {
        /**
         * Make use of the read of operations of the REST API.
         */
        API_READ,

        /**
         * Create a cohort that includes the protected resource.
         */
        BUILD_COHORT,

        /**
         * Show summary statistics of the resource (histogram of the
         * distribution and demographic information).
         */
        SHOW_SUMMARY_STATISTICS,

        /**
         * Show the protected resource in the grid view.
         */
        SHOW_IN_TABLE,

        /**
         * Export any part of the protected resource.
         */
        EXPORT,

        /**
         * Run an analysis including data from the protected resource
         */
        RUN_ANALYSIS,
    }
}
