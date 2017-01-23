package org.transmartproject.batch.db

import org.springframework.beans.factory.annotation.Value

/**
 * Helper component for running different code paths depending on the database
 * type being used.
 */
class PerDbTypeRunner {

    public static final String DEFAULT_KEY = 'DEFAULT'

    @Value('#{environment.getProperty(\'batch.jdbc.driver\')}')
    String driverClassName

    def run(Map<String, Closure<Object>> spec, Object... args) {
        def keyToUse
        if (spec.containsKey(databaseKey)) {
            keyToUse = databaseKey
        } else if (spec.containsKey(DEFAULT_KEY)) {
            keyToUse = DEFAULT_KEY
        } else {
            throw new UnsupportedOperationException(
                    "Operation not supported on $databaseKey")
        }

        if (spec[keyToUse].maximumNumberOfParameters == 0) {
            spec[keyToUse]()
        } else {
            spec[keyToUse](args)
        }
    }

    @SuppressWarnings('PrivateFieldCouldBeFinal')
    @Lazy private String databaseKey = {
        switch (driverClassName) {
            case 'org.postgresql.Driver':
                return 'postgresql'
            case 'oracle.jdbc.driver.OracleDriver':
                return 'oracle'
            default:
                throw new UnsupportedOperationException("Not supported: $driverClassName")
        }
    }()
}
