package org.transmartproject.batch.clinical.variable

import groovy.transform.CompileStatic
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper
import org.springframework.batch.item.file.mapping.FieldSetMapper
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.validation.BindException
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.patient.DemographicVariable

/**
 * Fill the calculated fields of {@link ClinicalVariable}.
 */
@Component
@JobScopeInterfaced
@CompileStatic
class ClinicalVariableFieldMapper implements FieldSetMapper<ClinicalVariable> {

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNodePath

    private final FieldSetMapper<ClinicalVariable> delegate =
            new BeanWrapperFieldSetMapper<>(
                    targetType: ClinicalVariable,
                    strict: false /* allow unmappable columns 5, 6 and 7 */)

    @Override
    ClinicalVariable mapFieldSet(FieldSet fieldSet) throws BindException {
        ClinicalVariable item = delegate.mapFieldSet(fieldSet)
        process item
    }

    private ClinicalVariable process(ClinicalVariable item) throws Exception {
        if (!ClinicalVariable.RESERVED.contains(item.dataLabel)) {
            ConceptPath path = topNodePath +
                    ConceptFragment.decode(item.categoryCode) +
                    ConceptFragment.decode(item.dataLabel)

            item.conceptPath = path
        }

        item.demographicVariable =
                DemographicVariable.getMatching(item.dataLabel)

        item
    }


}
