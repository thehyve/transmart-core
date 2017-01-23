package org.transmartproject.core.dataquery.clinical

import org.transmartproject.core.concept.ConceptKey

public interface ClinicalVariable {

    /**`
     * Data associated with a specific concept. The concept must be terminal,
     * i.e., it must have data associated directly to it, as opposed to
     * through children.
     *
     * Parameters: concept_code => <string representing the concept code.
     *                              Generally, but not necessarily, an integer
     *                              stored as a String>
     *             XOR
     *             concept_path => <string for the concept's path. The i2b2's
     *                              term full name; provided the term refers
     *                              to a concept (always the case with
     *                              tranSMART)>
     *
     * Type of data: {@link String} or {@link BigDecimal}.
     */
    public final static String TERMINAL_CONCEPT_VARIABLE = 'terminal_concept_variable'

    /**
     * TranSMART stores categorical variables as a container concept with as many
     * text-only leaf concepts as possible variables the categorical variable may
     * take. Each patient has a non-null value for at most one of those leaf
     * sub-concepts, with a value equal to the name of the sub-concept. So, for
     * instance:
     *
     * <pre>
     * Sex |            Patient1  Patient2  Patient 3
     *     |_ male      null      male      male
     *     |_ female    female    null      null
     * </pre>
     *
     * The reason for this is not completely clear. Maybe it's because it allows
     * showing the concept code next to each possible value (in this case "male"
     * and "female"). It also allows slightly easier selection of patients with
     * a given value (one can just drag the concept instead of having to constrain
     * it to a specific value).
     *
     * This type of clinical variable represents a collapsed view the terminal
     * concepts responsible for implementing a tranSMART "categorical variable".
     * In the example above, it would return "female" for "Patient1" and "male"
     * for "Patient2" and "Patient3".
     *
     * Parameters: concept_code => <string representing the concept code of
     *                              the container concept related to the
     *                              categorical variable>
     *             XOR
     *             concept_path => <string for the concept's path. The i2b2's
     *                              term full name; provided the term refers
     *                              to a concept (always the case with
     *                              tranSMART). The concept must be that of a
     *                              container concept related to a categorical
     *                              variable>
     *
     * If the passed concept does not represent a categorical variable's
     * container concept, the result is undefined.
     *
     * Variables of this type implement {@link ComposedVariable}.
     * Type of data: {@link String}.
     */
    public final static String CATEGORICAL_VARIABLE = 'categorical_variable'

    /**
     * Creates a variable that aggregates all the descendant 1) numerical leaf
     * variable terms and 2) categorical variables (see
     * {@link #CATEGORICAL_VARIABLE}) of the configured ontology term.
     * "Descendant", as used here, includes also the passed ontology term.
     *
     * Parameters: concept_code => <string representing the concept code of
     *                              any concept>
     *             XOR
     *             concept_path => <string for the concept's path. The i2b2's
     *                              term full name; provided the term refers
     *                              to a concept (always the case with
     *                              tranSMART)>
     *
     * Variables of this type implement {@link ComposedVariable}.
     * Type of data: {@link Map} from {@link ClinicalVariable} to {@link String}
     * or {@link BigDecimal}.
     */
    public final static String NORMALIZED_LEAFS_VARIABLE = 'normalized_leafs_variable'

    /**
     * @return the concept key. See {@link org.transmartproject.core.ontology.OntologyTerm#getKey()}
     */
    public ConceptKey getKey()

}
