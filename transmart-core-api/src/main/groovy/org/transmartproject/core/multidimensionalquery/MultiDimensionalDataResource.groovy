package org.transmartproject.core.multidimensionalquery

interface MultiDimensionalDataResource {

    /**
     *
     * @param args
     * @param dataType
     * @return
     */
    Hypercube retrieveData(Map args, String dataType)
}