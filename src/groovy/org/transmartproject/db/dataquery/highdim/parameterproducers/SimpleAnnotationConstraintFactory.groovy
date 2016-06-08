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
    private List<String> requiredParams = ['property','term','concept_key']

    @ProducerFor(DataConstraint.ANNOTATION_CONSTRAINT)
    DataConstraint createAnnotationConstraint(Map<String, Object> params) {

        if (requiredParams.inject(true, {result, i -> result && params.containsKey(i)})) {

            def concept_path = keyToPath(params['concept_key'])
            DetachedCriteria dc = DetachedCriteria.forClass(annotationClass)
            dc.setProjection(Projections.distinct(Projections.property('id')))
            dc.add(Restrictions.eq(params['property'], params['term']))
            dc.add(Restrictions.eq('platform', DeSubjectSampleMapping.findByConceptCode(ConceptDimension.findByConceptPath(concept_path).getConceptCode()).getPlatform()))
            return new SubqueryInDataConstraint (
                    field: this.field + '.id',
                    detachedCriteria: dc
            )
        }
        else
            throw new InvalidArgumentsException("SimpleAnnotationDataConstraint needs the following parameters: $requiredParams, but got $params")
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
