package org.transmartproject.db.tree

import groovy.transform.CompileStatic
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.query.AndConstraint
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.DimensionMetadata
import org.transmartproject.db.multidimquery.query.FieldConstraint
import org.transmartproject.db.multidimquery.query.ModifierConstraint
import org.transmartproject.db.multidimquery.query.Operator
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.multidimquery.query.StudyNameConstraint

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.MODIFIER_LEAF

@CompileStatic
class TreeNodeConstraintHelper {

    /**
     * Builds a constraint for a tree node.
     * @param node
     * @return
     */
    static Constraint createConstraintForTreeNode(TreeNodeImpl node) {
        if (node.studyNode && node.studyId) {
            return new StudyNameConstraint(studyId: node.studyId)
        }
        if (!(LEAF in node.visualAttributes || MODIFIER_LEAF in node.visualAttributes)) {
            return null
        }
        switch (node.tableName) {
            case 'concept_dimension':
                if (node.columnName == 'concept_path' && node.hasOperator(['=', 'like'])) {
                    // constraint for the concept
                    def conceptConstraint = new ConceptConstraint(path: node.dimensionCode)
                    // lookup study for this node
                    def parentStudyId = node.study?.studyId
                    if (!parentStudyId) {
                        // not a study specific node, return concept constraint only
                        return conceptConstraint
                    }
                    // combine concept constraint with study constraint
                    def studyConstraint = new StudyNameConstraint(studyId: parentStudyId)
                    return new AndConstraint(args: [conceptConstraint, studyConstraint])
                }
                return null
            case 'patient_dimension':
                if (node.columnName == 'patient_num') {
                    def patientIds = [] as Set<Long>
                    if (node.hasOperator(['='])) {
                        try {
                            patientIds.add(Long.parseLong(node.dimensionCode))
                        } catch(NumberFormatException e) {
                            return null
                        }
                    } else if (node.hasOperator(['in'])) {
                        try {
                            node.dimensionCode.split(',').each { String s ->
                                patientIds.add(Long.parseLong(s))
                            }
                        } catch(NumberFormatException e) {
                            return null
                        }
                    } else {
                        return null
                    }
                    return new PatientSetConstraint(patientIds: patientIds)
                }
                return null
            case 'modifier_dimension':
                if (node.columnName == 'modifier_path' && node.hasOperator(['=', 'like'])) {
                    return new ModifierConstraint(path: node.dimensionCode)
                } else if (node.columnName == 'modifier_cd' && node.hasOperator(['=', 'like'])) {
                    return new ModifierConstraint(modifierCode: node.dimensionCode)
                }
                return null
            case 'trial_visit_dimension':
                def fieldName
                def value
                switch (node.columnName) {
                    case 'rel_time_label':
                        fieldName = 'relTimeLabel'
                        value = node.dimensionCode
                        break
                    case 'trial_visit_num':
                        fieldName = 'id'
                        try {
                            value = Long.parseLong(node.dimensionCode)
                        } catch (NumberFormatException e) {
                            return null
                        }
                        break
                    default:
                        return null
                }
                return new FieldConstraint(
                        field: DimensionMetadata.getField(DimensionImpl.TRIAL_VISIT.name, fieldName),
                        operator: Operator.EQUALS,
                        value: value)
            default:
                return null
        }

    }
}
