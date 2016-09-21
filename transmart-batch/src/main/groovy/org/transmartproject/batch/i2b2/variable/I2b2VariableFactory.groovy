package org.transmartproject.batch.i2b2.variable

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.i2b2.database.I2b2Tables

/**
 * Creates {@link I2b2Variable}s.
 */
@Component
@Scope('singleton')
class I2b2VariableFactory {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    private I2b2Tables tables

    private final List<DimensionI2b2Variable> dimensionVariables = [
            PatientDimensionI2b2Variable.values(),
            VisitDimensionI2b2Variable.values(),
            ProviderDimensionI2b2Variable.values()
    ].flatten()

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    I2b2Variable create(String variableLabel,
                        I2b2Variable lastVariable /* on the same file */) {
        def result
        result = dimensionVariables.find { DimensionI2b2Variable it ->
            it.key.equalsIgnoreCase(variableLabel) ? it : null
        }

        if (result) {
            return result
        }

        result = DimensionExternalIdI2b2Variable.values().find {
            it.key.equalsIgnoreCase(variableLabel) ? it : null
        }

        if (result) {
            return result
        }

        if (variableLabel.toLowerCase(Locale.ENGLISH).startsWith('con:')) {
            if (variableLabel.size() < 5) {
                throw new IllegalArgumentException("Malformed concept " +
                        "variable label (too short): $variableLabel")
            }

            String conceptCode,
                   conceptPath // may stay null

            if (variableLabel[4] == '\\') {
                conceptPath = new ConceptPath(variableLabel[4..-1])
                conceptCode = resolveConceptFullName(conceptPath)
            } else {
                conceptCode = variableLabel[4..-1]
                validateConceptCode conceptCode
            }

            return new ConceptI2b2Variable(
                    conceptCode: conceptCode,
                    conceptPath: conceptPath)
        }

        if (variableLabel.toLowerCase(Locale.ENGLISH).startsWith('mod:')) {
            if (variableLabel.size() < 5) {
                throw new IllegalArgumentException("Malformed concept " +
                        "variable label (too short): $variableLabel")
            }

            ConceptI2b2Variable lastConceptVariable

            if (lastVariable instanceof ConceptI2b2Variable) {
                lastConceptVariable = lastVariable
            } else if (lastVariable instanceof ModifierI2b2Variable) {
                lastConceptVariable = lastVariable.boundConceptVariable
            } else { // could also be null
                throw new IllegalArgumentException('Modifier column mapping ' +
                        'entries can only appear immediately after the ' +
                        'concept variables they qualify or other modifiers. ' +
                        'It appeared after: ' + lastVariable)
            }

            def modifierCode = variableLabel[4..-1]
            validateModifierCode modifierCode
            return new ModifierI2b2Variable(
                    boundConceptVariable: lastConceptVariable,
                    modifierCode: modifierCode)
        }

        if (variableLabel.equalsIgnoreCase('start_date')) {
            return ObservationDateI2b2Variable.START_DATE
        }

                if (variableLabel.equalsIgnoreCase('end_date')) {
            return ObservationDateI2b2Variable.END_DATE
        }

        throw new IllegalArgumentException(
                "Unrecognized variable label: $variableLabel")
    }

    private String resolveConceptFullName(ConceptPath conceptFullName) {
        try {
            jdbcTemplate.queryForObject """
                    SELECT concept_cd FROM $tables.conceptDimension
                    WHERE concept_path = :conceptPath
                    """, [conceptPath: conceptFullName.toString()], String
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException(
                    "No concept with path $conceptFullName", e)
        }
    }

    private void validateConceptCode(String conceptCode) {
        def count = jdbcTemplate.queryForObject """
                SELECT COUNT(*) FROM $tables.conceptDimension
                WHERE concept_cd = :conceptCode
                """, [conceptCode: conceptCode], Long
        if (count == 0) {
            throw new IllegalArgumentException(
                    "No concepts with code $conceptCode")
        }
    }

    private String validateModifierCode(String modifierCode) {
        def count = jdbcTemplate.queryForObject """
                SELECT COUNT(*) FROM $tables.modifierDimension
                WHERE modifier_cd = :modifierCode
                """, [modifierCode: modifierCode], Long
        if (count == 0) {
            throw new IllegalArgumentException(
                    "No modifiers with code $modifierCode")
        }

    }
}
