package org.transmartproject.core.dataquery.clinical

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.QueryResult

/**
 * Entry point for retrieving clinical data.
 */
public interface ClinicalDataResource {

    /**
     * Equivalent to {@link #retrieveData(java.util.List, java.util.List)}
     * called with <code>[patientSet]</code> as the first argument.
     *
     * @param patientSet the result set containing the patients to include in
     * the result (lines)
     * @param variables the list of variables to include in the result (columns)
     * @throws InvalidArgumentsException if no variables are specified
     * @return the result set
     */
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(QueryResult patientSet,
                                                                   List<ClinicalVariable> variables)

    /**
     * Retrieves all the data pertaining to the patients included in the passed
     * result set and the passed variables.
     *
     * The result set may have more columns than the variables passed in. This
     * is because several variables are composite variables that are expanded
     * into terminal variables. See the documentation of
     * {@link ClinicalVariable#NORMALIZED_LEAFS_VARIABLE} and
     * {@link ClinicalVariable#CATEGORICAL_VARIABLE}. If you use these
     * variables, you cannot rely on a variable column being in a specific
     * position in the result set. You are guaranteed that the variables will
     * not be reordered, though. Note that this should not be a problem if you
     * fetch cells from the columns using the variables as indices, rather than
     * using integers.
     *
     * An empty result set will be returned iif the passed patient sets contain
     * no patients. If the patients simply have no data for the given variables,
     * empty lines (containing only nulls) will be returned.
     *
     * @param patientSets the result sets containing the patients to include in
     * the result (lines). Patients will be taken from all the result sets
     * (union)
     * @param variables the list of variables to include in the result (columns)
     * @throws InvalidArgumentsException if no variables are specified
     * @return the result set
     */
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(List<QueryResult> patientSets,
                                                                   List<ClinicalVariable> variables)

    /**
     * Overload of {@link #retrieveData(java.util.List, java.util.List)} that
     * takes the patients directly, rather than a patient {@link QueryResult}
     * or a list of them.
     *
     * @param patientSets the result sets containing the patients to include in
     * the result (lines). Patients will be taken from all the result sets
     * (union)
     * @param variables the list of variables to include in the result (columns)
     * @throws InvalidArgumentsException if no variables are specified
     * @return the result set
     */
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(Set<Patient> patients,
                                                                   List<ClinicalVariable> ontologyTerms)

    /**
     * Retrieves the data for passed variables for the patients included in the passed
     * study.
     *
     * @param study name of a study that returns patients for
     * @param variables the list of variables to include in the result (columns)
     * @throws InvalidArgumentsException if no variables are specified
     * @return the result set
     */
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(Study study,
                                                                   List<ClinicalVariable> ontologyTerms)

    /**
     * Creates a clinical variable to pass to the <code>retrieveData</code>
     * methods.
     *
     * @see #retrieveData(java.util.List, java.util.List)
     * @param parameters variable type specific string -> object map
     * @param type the type of clinical variable to create. See
     * {@link ClinicalVariable} for a list of available types.
     *
     * @return the created variable
     * @throws InvalidArgumentsException if the type does not exist or the
     * arguments are incorrect
     */
    ClinicalVariable createClinicalVariable(Map<String, Object> parameters,
                                            String type) throws InvalidArgumentsException

}
