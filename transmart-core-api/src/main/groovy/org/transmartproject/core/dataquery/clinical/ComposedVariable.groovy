package org.transmartproject.core.dataquery.clinical

/**
 * A {@link ClinicalVariable} that aggregates several {@link ClinicalVariable}s.
 */
interface ComposedVariable extends ClinicalVariable {

    /**
     * The {@link ClinicalVariable}s that compose this object.
     * @return the list of inner clinical variables
     */
    List<ClinicalVariable> getInnerClinicalVariables()

}
