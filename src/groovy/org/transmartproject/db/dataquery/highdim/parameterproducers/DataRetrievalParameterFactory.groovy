package org.transmartproject.db.dataquery.highdim.parameterproducers
/**
 * Defines the interface for beans implementing producers that take a constraint
 * or projection name and its parameters and create the corresponding constraint
 * or projection.
 */
interface DataRetrievalParameterFactory {

    Set<String> getSupportedNames()

    boolean supports(String name)

    /**
     * Returns the created object or null if the name is not recognized.
     * @param name
     * @param params
     * @return
     */
    def createFromParameters(String name, Map<String, Object> params)

}
