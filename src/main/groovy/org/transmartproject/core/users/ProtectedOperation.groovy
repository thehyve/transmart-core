package org.transmartproject.core.users

interface ProtectedOperation {

    enum WellKnownOperations implements ProtectedOperation {

        /**
         * Make use of the read of operations of the REST API.
         */
        API_READ

    }
}
