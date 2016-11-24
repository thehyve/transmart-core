package org.transmartproject.db.tree

import groovy.transform.CompileStatic
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermType
import org.transmartproject.db.multidimquery.TrialVisitDimension
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.FieldConstraint
import org.transmartproject.db.multidimquery.query.ModifierConstraint
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.multidimquery.query.StudyConstraint
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

    TreeNode(I2b2Secure term, List<TreeNode> children) {
        this.delegate = term
        this.name = term.name
        this.fullName = term.fullName
        this.visualAttributes = term.visualAttributes
        this.children = children
    }

    boolean hasOperator(List<String> operators) {
        delegate.operator.toLowerCase().trim() in operators
    }

    String getDimensionCode () {
        delegate.dimensionCode
    }

    String getTableName() {
        delegate.dimensionTableName.toLowerCase().trim()
    }

    String getColumnName() {
        delegate.columnName.toLowerCase().trim()
    }

    /**
     * Create a constraint object for the query that this node represents.
     * @return
     */
    Map getConstraint() {
        def constraint = [:] as Map
        switch (tableName) {
            case 'concept_dimension':
                if (columnName == 'concept_path' && hasOperator(['=', 'like'])) {
                    constraint.type = ConceptConstraint.simpleName
                    constraint.path = dimensionCode
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
                        constraint.type = StudyConstraint.simpleName
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

    boolean isHighDim() {
        HIGH_DIMENSIONAL in visualAttributes
    }

    List<TreeNode> getChildren() {
        children
    }

    private Map<String, Object> metadataMap

    Map<String, Object> getMetadata() {
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

    OntologyTermType getOntologyTermType() {
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
