/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.tree

import groovy.transform.CompileStatic
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermType
import org.transmartproject.core.tree.TreeNode
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.query.Combination
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.FieldConstraint
import org.transmartproject.db.multidimquery.query.ModifierConstraint
import org.transmartproject.db.multidimquery.query.Operator
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.ontology.I2b2Secure

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.*

@CompileStatic
class TreeNodeImpl implements TreeNode {

    I2b2Secure delegate

    String apiVersion

    TreeNode parent

    List<TreeNode> children

    List<OntologyTermTag> tags

    Long observationCount

    Long patientCount

    String name

    String fullName

    EnumSet<OntologyTerm.VisualAttributes> visualAttributes

    String dimension
    
    String conceptPath

    TreeNodeImpl(I2b2Secure term, List<TreeNode> children) {
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

    final Constraint getConstraint() {
        TreeNodeConstraintHelper.createConstraintForTreeNode(this)
    }

    final String getStudyId() {
        if (!studyNode) {
            return null
        }
        if (tableName == 'study' && columnName == 'study_id' && hasOperator(['=', 'like'])) {
            return dimensionCode
        } else if (tableName == 'concept_dimension') {
            // FIXME: This is not a very reliable method to get the study id,
            // but it is required if a study has to be compatible with transmartApp.
            // If table name 'study' is specified, dragging a study node into patient selection
            // does not work.
            return delegate.sourceSystemCd
        }
        return null
    }

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
        } else if (studyNode) {
            OntologyTermType.STUDY
        } else if (NUMERICAL in visualAttributes) {
            OntologyTermType.NUMERIC
        } else if (CATEGORICAL_OPTION in visualAttributes) {
            OntologyTermType.CATEGORICAL_OPTION
        } else if (CATEGORICAL in visualAttributes) {
            OntologyTermType.CATEGORICAL
        } else if (DATE in visualAttributes) {
            OntologyTermType.DATE
        } else if (TEXT in visualAttributes) {
            OntologyTermType.TEXT
        } else {
            OntologyTermType.UNKNOWN
        }
    }
}
