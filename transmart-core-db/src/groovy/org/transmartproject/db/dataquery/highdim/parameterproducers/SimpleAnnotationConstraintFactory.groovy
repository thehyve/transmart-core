package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.dataconstraints.SubqueryInDataConstraint
import org.transmartproject.db.i2b2data.ConceptDimension

/**
 * Author: Denny Verbeeck (dverbeec@its.jnj.com)
 */
class SimpleAnnotationConstraintFactory extends AbstractMethodBasedParameterFactory {
    String field
    Class annotationClass

    @ProducerFor(DataConstraint.ANNOTATION_CONSTRAINT)
    DataConstraint createAnnotationConstraint(Map<String, Object> params) {

        if (!params.keySet().containsAll(['property','term']) ||
                !(params.keySet().contains('concept_code') || params.keySet().contains('concept_key')))
            throw new InvalidArgumentsException("SimpleAnnotationDataConstraint needs the following parameters: ['property','term','concept_key' OR 'concept_code'], but got $params")
        DetachedCriteria dc = DetachedCriteria.forClass(annotationClass)
        dc.setProjection(Projections.distinct(Projections.property('id')))
        dc.add(Restrictions.eq(params['property'], params['term']))
        if (params.containsKey('concept_code')) {
            dc.add(Restrictions.eq('platform', DeSubjectSampleMapping.findByConceptCode(params['concept_code']).getPlatform()))
        }
        else if (params.containsKey('concept_key')) {
            def concept_path = keyToPath(params['concept_key'])
            dc.add(Restrictions.eq('platform', DeSubjectSampleMapping.findByConceptCode(ConceptDimension.findByConceptPath(concept_path).getConceptCode()).getPlatform()))
        }
        return new SubqueryInDataConstraint (
                field: this.field + '.id',
                detachedCriteria: dc
        )

    }

    private def keyToPath(String concept_key) {
        String fullname = concept_key.substring(concept_key.indexOf("\\", 2), concept_key.length());
        String path = fullname;
        if (!fullname.endsWith("\\")) {
            path = path + "\\";
        }
        return path;
    }
}
