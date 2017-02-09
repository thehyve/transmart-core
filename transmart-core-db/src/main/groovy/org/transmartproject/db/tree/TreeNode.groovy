/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.tree

import groovy.transform.CompileStatic
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermType
import org.transmartproject.db.multidimquery.TrialVisitDimension
import org.transmartproject.db.multidimquery.query.Combination
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.FieldConstraint
import org.transmartproject.db.multidimquery.query.ModifierConstraint
import org.transmartproject.db.multidimquery.query.Operator
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.ontology.I2b2Secure

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.*

@CompileStatic
class TreeNode {

    public I2b2Secure delegate

    public String apiVersion

    public TreeNode parent

    private List<TreeNode> children

    public List<OntologyTermTag> tags

    public Long observationCount

    public Long patientCount

    public String name

    public String fullName

    public EnumSet<OntologyTerm.VisualAttributes> visualAttributes

    public String dimension

    TreeNode(I2b2Secure term, List<TreeNode> children) {
        this.delegate = term
        this.name = term.name
        this.fullName = term.fullName
        this.visualAttributes = term.visualAttributes
        this.children = children
    }

    final boolean hasOperator(List<String> operators) {
        delegate.operator.toLowerCase().trim() in operators
    }

    final String getDimensionCode () {
        delegate.dimensionCode
    }

    final String getTableName() {
        delegate.dimensionTableName.toLowerCase().trim()
    }

    final String getColumnName() {
        delegate.columnName.toLowerCase().trim()
    }

    /**
     * Create a constraint object for the query that this node represents.
     *
     * Supports study, concept, modifier, patient and trial visit nodes.
     *
     * @return the constraint map for this node.
     */
    final Map getConstraint() {
        def constraint = [:] as Map
        if (studyNode && studyId) {
            return [
                type: StudyNameConstraint.simpleName,
                studyId: studyId
            ] as Map
        }
        if (!(LEAF in visualAttributes || MODIFIER_LEAF in visualAttributes)) {
            return null
        }
        switch (tableName) {
            case 'concept_dimension':
                if (columnName == 'concept_path' && hasOperator(['=', 'like'])) {
                    // constraint for the concept
                    def conceptConstraint = [
                            type: ConceptConstraint.simpleName,
                            path: dimensionCode
                    ] as Map
                    // lookup study for this node
                    def parentStudyId = study?.studyId
                    if (!parentStudyId) {
                        // not a study specific node, return concept constraint only
                        return conceptConstraint
                    }
                    // combine concept constraint with study constraint
                    def studyConstraint =                             [
                            type: StudyNameConstraint.simpleName,
                            studyId: parentStudyId
                    ]
                    constraint.type = Combination.simpleName
                    constraint.operator = Operator.AND.symbol
                    constraint.args = [
                            conceptConstraint,
                            studyConstraint
                    ]
                    return constraint
                }
                return null
            case 'patient_dimension':
                if (columnName == 'patient_num') {
                    constraint.type = PatientSetConstraint.simpleName
                    def patientIds = []
                    if (hasOperator(['='])) {
                        try {
                            patientIds.add(Long.parseLong(dimensionCode))
                        } catch(NumberFormatException e) {
                            return null
                        }
                    } else if (hasOperator(['in'])) {
                        try {
                            dimensionCode.split(',').each { String s ->
                                patientIds.add(Long.parseLong(s))
                            }
                        } catch(NumberFormatException e) {
                            return null
                        }
                    } else {
                        return null
                    }
                    constraint.patientIds = [dimensionCode]
                    return constraint
                }
                return null
            case 'modifier_dimension':
                constraint.type = ModifierConstraint.simpleName
                if (columnName == 'modifier_path' && hasOperator(['=', 'like'])) {
                    constraint.path = dimensionCode
                    return constraint
                } else if (columnName == 'modifier_cd' && hasOperator(['=', 'like'])) {
                    constraint.modifierCode = dimensionCode
                    return constraint
                }
                return null
            case 'trial_visit_dimension':
                constraint.type = FieldConstraint.simpleName
                def fieldName
                def value
                switch (columnName) {
                    case 'study_num':
                        constraint.type = StudyNameConstraint.simpleName
                        if (hasOperator(['='])) {
                            try {
                                constraint.id = Long.parseLong(dimensionCode)
                            } catch (NumberFormatException e) {
                                return null
                            }
                            return constraint
                        }
                        return null
                    case 'rel_time_label':
                        fieldName = 'relTimeLabel'
                        value = dimensionCode
                        break
                    case 'trial_visit_num':
                        fieldName = 'id'
                        try {
                            value = Long.parseLong(dimensionCode)
                        } catch (NumberFormatException e) {
                            return null
                        }
                        break
                    default:
                        return null
                }
                constraint.field = [
                        dimension: TrialVisitDimension.simpleName,
                        fieldName: fieldName,
                        value: value
                ] as Map
                return constraint
            default:
                return null
        }
    }

    /**
     * Retrieves the dimension code as study id if the node is a study node,
     * the table name is 'study' and the column name is 'study_id';
     * null otherwise.
     */
    final String getStudyId() {
        if (!studyNode) {
            return null
        }
        if (tableName == 'study' && columnName == 'study_id' && hasOperator(['=', 'like'])) {
            return dimensionCode
        }
        return null
    }

    /**
     * Returns the node itself if itself is a study node or an ancestry study node if it exists.
     */
    final TreeNode getStudy() {
        if (studyNode) {
            this
        } else {
            parent?.study
        }
    }

    final boolean isStudyNode() {
        STUDY in visualAttributes
    }

    final boolean isHighDim() {
        HIGH_DIMENSIONAL in visualAttributes
    }

    final List<TreeNode> getChildren() {
        children
    }

    private Map<String, Object> metadataMap

    final Map<String, Object> getMetadata() {
        if (!delegate.metadataxml)
            return null

        if (metadataMap) {
            return metadataMap
        }

        def slurper = new XmlSlurper().parseText(delegate.metadataxml)
        metadataMap = [:] as Map<String, Object>

        metadataMap.okToUseValues = slurper['Oktousevalues'] == 'Y'
        metadataMap.unitValues = [
                normalUnits: slurper['UnitValues']?.getAt('NormalUnits')?.toString(),
                equalUnits: slurper['UnitValues']?.getAt('EqualUnits')?.toString()
        ] as Map<String, String>

        def seriesMeta = slurper['SeriesMeta']
        if (seriesMeta) {
            metadataMap.seriesMeta = [
                    unit: seriesMeta['Unit']?.toString(),
                    value: seriesMeta['Value']?.toString(),
                    label: seriesMeta['DisplayName']?.toString()
            ] as Map<String, String>
        }
        metadataMap
    }

    final OntologyTermType getOntologyTermType() {
        if (highDim) {
            OntologyTermType.HIGH_DIMENSIONAL
        } else if (STUDY in visualAttributes) {
            OntologyTermType.STUDY
        } else if (metadata?.get('okToUseValues')) {
            OntologyTermType.NUMERIC
        } else if (LEAF in visualAttributes) {
            OntologyTermType.CATEGORICAL_OPTION
        } else {
            OntologyTermType.UNKNOWN
        }
    }
}
