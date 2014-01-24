package org.transmartproject.db.dataquery.highdim.correlations

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException

import javax.annotation.PostConstruct

@Component
class CorrelationTypesRegistry {

    /* R: target type, C: source type, V: correlation type */
    Table<String, String, CorrelationType> registryTable = HashBasedTable.create()

    /* source type -> data constraint name */
    Map<String, String> constraintMap = [:]

    @PostConstruct
    void init() {
        registerKnownTypes()
        registerKnownConstraints()
    }

    private void registerKnownTypes() {
        registryTable.metaClass.leftShift = { delegate.put it.targetType, it.sourceType, it }

        registryTable << new CorrelationType(name: 'GENE',           sourceType: 'GENE',        targetType: 'GENE')

        /* actually the correlation name in BIO_MARKER_CORREL_MV_VIEW is
         * 'Protein' (only capitalized). The inconsistency is unacceptable, so
         * I'll assume an uppercase name like everywhere else and hope the stuff
         * will be sorted out in the DB */
        registryTable << new CorrelationType(name: 'PROTEIN',         sourceType: 'PROTEIN',    targetType: 'PROTEIN')

        registryTable << new CorrelationType(name: 'METABOLITE',      sourceType: 'METABOLITE', targetType: 'METABOLITE')

        /* no example data for these; not sure the sourceType is correct */
        registryTable << new CorrelationType(name: 'PATHWAY GENE',    sourceType: 'PATHWAY',    targetType: 'GENE')
        registryTable << new CorrelationType(name: 'HOMOLOGENE_GENE', sourceType: 'HOMOLOGENE', targetType: 'GENE')

        /* yes, space instead of underscore and extra TO word... */
        registryTable << new CorrelationType(name: 'PROTEIN TO GENE', sourceType: 'PROTEIN',    targetType: 'GENE')
        registryTable << new CorrelationType(name: 'GENE TO PROTEIN', sourceType: 'GENE',       targetType: 'PROTEIN')
        registryTable << new CorrelationType(name: 'PATHWAY TO PROTEIN', sourceType: 'PATHWAY', targetType: 'PROTEIN')

        registryTable << new CorrelationType(
                name:             'GENE_SIGNATURE_ITEM',
                sourceType:       'GENESIG',
                targetType:       'GENE',
                correlationTable: 'SEARCHAPP.SEARCH_BIO_MKR_CORREL_VIEW',
                leftSideColumn:   'DOMAIN_OBJECT_ID')

        registryTable << new CorrelationType(
                name:             'SUPERPATHWAY TO METABOLITE',
                sourceType:       'METABOLITE_SUPERPATHWAY',
                targetType:       'METABOLITE',
                correlationTable: 'BIOMART.BIO_METAB_SUPERPATHWAY_VIEW',
                leftSideColumn:   'SUPERPATHWAY_ID')

        registryTable << new CorrelationType(
                name:             'SUBPATHWAY TO METABOLITE',
                sourceType:       'METABOLITE_SUBPATHWAY',
                targetType:       'METABOLITE',
                correlationTable: 'BIOMART.BIO_METAB_SUBPATHWAY_VIEW',
                leftSideColumn:   'SUBPATHWAY_ID')
    }

    private void registerKnownConstraints() {
        constraintMap.GENE = DataConstraint.GENES_CONSTRAINT
        constraintMap.PROTEIN = DataConstraint.PROTEINS_CONSTRAINT
        constraintMap.PATHWAY = DataConstraint.PATHWAYS_CONSTRAINT
        constraintMap.HOMOLOGENE = 'homologenes'
        constraintMap.GENESIG = DataConstraint.GENE_SIGNATURES_CONSTRAINT
        constraintMap.METABOLITE = 'metabolites'
        constraintMap.METABOLITE_SUBPATHWAY = 'metabolite_subpathways'
        constraintMap.METABOLITE_SUPERPATHWAY = 'metabolite_superpathways'
    }


    void registerCorrelation(CorrelationType correlationType) {
        registryTable << correlationType
    }

    void registerConstraint(String sourceType, String constraintName) {
        constraintMap[sourceType] = constraintName
    }

    Set<CorrelationType> getCorrelationTypesForTargetType(String targetType) {
        registryTable.row(targetType).values()
    }

    /* constraint name -> correlation type for source type of constraint and
     *                    specified target type */
    Map<String, CorrelationType> getConstraintsForTargetType(String targetType) {
        registryTable.row(targetType).collectEntries { String sourceType,
                                                       CorrelationType correlation ->
            if (!constraintMap[sourceType]) {
                throw new InvalidArgumentsException("Source type $sourceType " +
                        "has no registered constraint name")
            }

            [ constraintMap[sourceType], correlation ]
        }
    }

}
