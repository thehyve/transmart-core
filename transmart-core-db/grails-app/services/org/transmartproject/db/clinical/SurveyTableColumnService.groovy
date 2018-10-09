package org.transmartproject.db.clinical

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.MetadataAwareDataColumn
import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.ontology.VariableMetadata
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.HypercubeDataColumn
import org.transmartproject.db.multidimquery.SurveyTableView
import org.transmartproject.db.support.ParallelPatientSetTaskService

import static org.transmartproject.core.ontology.Measure.NOMINAL
import static org.transmartproject.core.ontology.VariableDataType.STRING

class SurveyTableColumnService {

    @Autowired
    MDStudiesResource studiesResource

    @Autowired
    ConceptsResource conceptsResource

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    AggregateDataResource aggregateDataResource

    @Canonical
    static class StudyConceptPair {
        MDStudy study
        String conceptCode
    }

    @CompileStatic
    static class DataColumnComparator<C extends DataColumn> implements Comparator<C> {
        @Override
        int compare(C a, C b) {
            a.label <=> b.label
        }
    }

    static final Comparator<MetadataAwareDataColumn> dataColumnComparator = new DataColumnComparator<MetadataAwareDataColumn>()

    private String getVariableName(HypercubeDataColumn originalColumn) {
        VariableMetadata varMeta = getStudyVariableMetadata(originalColumn)
        if (varMeta?.name) {
            return varMeta?.name
        } else {
            def conceptCode = (String)originalColumn.coordinates[DimensionImpl.CONCEPT]
            return conceptCode
        }
    }

    final static String MISSING_VALUE_MODIFIER_DIMENSION_NAME = 'missing_value'

    Dimension findMissingValueDimension() {
        multiDimService.getDimension(MISSING_VALUE_MODIFIER_DIMENSION_NAME)
    }

    VariableMetadata computeColumnMetadata(String conceptCode) {
        def concept = conceptsResource.getConceptByConceptCode(conceptCode)
        new VariableMetadata(
                type: STRING,
                measure: NOMINAL,
                description: concept.name,
                width: 25,
                columns: 25
        )
    }

    VariableMetadata getStudyVariableMetadata(HypercubeDataColumn originalColumn) {
        def study = studiesResource.getStudyByStudyId((String)originalColumn.coordinates[DimensionImpl.STUDY])
        def conceptCode = (String)originalColumn.coordinates[DimensionImpl.CONCEPT]
        study.metadata?.conceptCodeToVariableMetadata?.get(conceptCode)
    }

    ImmutableList<MetadataAwareDataColumn> getMetadataAwareColumns(List<HypercubeDataColumn> columns) {
        boolean includeMeasurementDateColumns = columns.any {
            String studyId = (String) it.coordinates[DimensionImpl.STUDY]
            if (!studyId) {
                return false
            }
            MDStudy study = studiesResource.getStudyByStudyId(studyId)
            DimensionImpl.START_TIME in study.dimensions
        }
        getMetadataAwareColumns(columns, includeMeasurementDateColumns)
    }

    ImmutableList<MetadataAwareDataColumn> getMetadataAwareColumns(List<HypercubeDataColumn> columns, boolean includeMeasurementDateColumns) {
        // check if any column contains a study with the start time dimension
        log.debug "Include measurement columns: ${includeMeasurementDateColumns}"

        List<MetadataAwareDataColumn> transformedColumns = []
        transformedColumns.add(new SurveyTableView.FisNumberColumn())
        for (HypercubeDataColumn originalColumn: columns) {
            String varName = getVariableName(originalColumn)
            def conceptCode = (String)originalColumn.coordinates[DimensionImpl.CONCEPT]
            def metadata = this.getStudyVariableMetadata(originalColumn) ?:
                    this.computeColumnMetadata(conceptCode)
            transformedColumns.add(new SurveyTableView.VariableColumn(varName, originalColumn, metadata, findMissingValueDimension()))
            if (includeMeasurementDateColumns) {
                transformedColumns.add(new SurveyTableView.MeasurementDateColumn("${varName}.date", originalColumn))
            }
        }
        transformedColumns.sort(dataColumnComparator)
        ImmutableList.copyOf(transformedColumns)
    }

    /**
     * Compute the data columns for a tabular view with columns based on studies and concepts.
     * The computed columns are either based on restrictions on studies and concepts (i.e.,
     * constraints of the shape <code>(study A and (concept 1 or concept 2)) or (...)</code>),
     * or on a computed aggregate of all studies and concept combinations for the patient set.
     *
     * If the column constraint is of the required shape, this will provide the list of
     * data columns directly, without having to consult the database.
     * If that is not the case, counts per study and concept are computed for the patient set,
     * which may be an expensive operation if that information is not in the application cache.
     *
     * @param patientSetConstraint the patient set to compute combinations of studies and concepts for.
     * @param columnConstraint the column constraint, restricting studies and concepts, to extract
     *        a column definition from.
     * @param user
     * @return a list of columns, each based on a study-concept pair. Or null in case it does not match the pattern.
     */
    private ImmutableList<HypercubeDataColumn> getColumnsForTableQuery(PatientSetConstraint patientSetConstraint, Constraint columnConstraint, User user) {
        def allStudyConceptPairs = getStudyAndConceptListFromCounts(patientSetConstraint, user)
        log.debug "Column query: ${columnConstraint.toJson()}"
        List<StudyConceptPair> studyConceptPairs = null
        if (columnConstraint instanceof Combination && columnConstraint.operator == Operator.AND) {
            def askedStudyConceptPairs = getStudyAndConceptFromConstraint(new AndConstraint(columnConstraint.args))
            studyConceptPairs = new ArrayList<>(allStudyConceptPairs)
            studyConceptPairs.retainAll(askedStudyConceptPairs)
        } else if (columnConstraint instanceof Combination && columnConstraint.operator == Operator.OR) {
            studyConceptPairs = getStudyAndConceptListFromConstraint(new OrConstraint(columnConstraint.args), allStudyConceptPairs)
        } else if (columnConstraint instanceof ConceptConstraint) {
            studyConceptPairs = getStudyAndConceptListForConcept((ConceptConstraint)columnConstraint, allStudyConceptPairs)
        } else if (columnConstraint instanceof StudyNameConstraint) {
            studyConceptPairs = getStudyAndConceptListForStudy((StudyNameConstraint)columnConstraint, allStudyConceptPairs)
        } else if (columnConstraint == null || columnConstraint instanceof TrueConstraint) {
            studyConceptPairs = allStudyConceptPairs
        }
        if (studyConceptPairs == null) {
            // no list of study-concept constraint pairs.
            log.debug("Don't know how to reduce number of study/concept pairs given the column constraint: ${columnConstraint}.")
            return null
        }
        toColumnList(studyConceptPairs)
    }

    /**
     * Compute the data columns for a tabular view with columns based on studies and concepts.
     * If possible, the constraint is split into a patient set part and a column constraint part.
     * The computed columns are then either based on restrictions on studies and concepts (i.e.,
     * column constraints of the shape <code>(study A and (concept 1 or concept 2)) or (...)</code>),
     * or on a computed aggregate of all studies and concept combinations for the patient set.
     *
     * If the constraint is splittable, the columns are based on the column restrictions.
     * If not, all combinations of studies and concepts for the entire constraint are computed.
     *
     * @param constraint the data set constraint.
     * @param user
     * @return a list of columns, each based on a study-concept pair.
     */
    ImmutableList<HypercubeDataColumn> getHypercubeDataColumnsForConstraint(Constraint constraint, User user) {
        def parts = ParallelPatientSetTaskService.getConstraintParts(constraint)
        if (parts.patientSetConstraint) {
            // Try to base the columns on selected concepts and studies
            def columns = getColumnsForTableQuery(parts.patientSetConstraint, parts.otherConstraint, user)
            if (columns != null) {
                return columns
            }
        }
        // Compute all combinations of studies and concepts for the constraint.
        ImmutableList.Builder<HypercubeDataColumn> columns = ImmutableList.builder()
        Map<String, Map<String, Counts>> countsPerStudyAndConcept = aggregateDataResource.countsPerStudyAndConcept(constraint, user)
        for (Map.Entry<String, Map> entry : countsPerStudyAndConcept.entrySet()) {
            String studyId = entry.key
            for (Map.Entry<String, Counts> conceptCodesToCountsEntry : entry.value.entrySet()) {
                String conceptCode = conceptCodesToCountsEntry.key
                Counts counts = conceptCodesToCountsEntry.value
                if (counts.observationCount > 0 || counts.patientCount > 0) {
                    ImmutableMap<Dimension, Object> coordinates = ImmutableMap.builder()
                            .put((Dimension) DimensionImpl.CONCEPT, (Object) conceptCode)
                            .put((Dimension) DimensionImpl.STUDY, (Object) studiesResource.getStudyByStudyId(studyId).name)
                            .build()
                    columns.add(new HypercubeDataColumn(coordinates))
                }
            }
        }
        columns.build()
    }

    /**
     * Get a list of study-concept combinations from a constraint.
     * E.g., <code>{type: 'and', args: [{type: 'concept', conceptCode: 'age'}, {type: 'study_name', studyId: 'GSE8581']}]}</code>
     * will result in the pair (study: GSE8581, concept: age), as a singleton list.
     * Disjunctions of such constraints will result in a list of such pairs.
     * If the constraint does not fit in this pattern (disjunction of conjunctions of a study_name and a concept constraint),
     * then the function returns null.
     *
     * @param constraint
     * @return a list of study-concept pairs if the pattern matches, null otherwise.
     */
    private List<StudyConceptPair> getStudyAndConceptFromConstraint(AndConstraint constraint) {
        def types = constraint.args.collect { it.class } as Set<Class>
        if (constraint.args.size() != 2 || types != ([ConceptConstraint, StudyNameConstraint] as Set<Class>)) {
            log.debug "Found combination of constraint types: ${types.toListString()}"
            return null
        }
        List<StudyConceptPair> result = []
        def studyConstraint = (StudyNameConstraint)constraint.args.find { it instanceof StudyNameConstraint }
        def study = studiesResource.getStudyByStudyId(studyConstraint.studyId)
        def conceptConstraint = (ConceptConstraint)constraint.args.find { it instanceof ConceptConstraint }
        if (conceptConstraint.conceptCode) {
            result << new StudyConceptPair(study, conceptConstraint.conceptCode)
        } else if (conceptConstraint.conceptCodes) {
            for (String conceptCode: conceptConstraint.conceptCodes) {
                result << new StudyConceptPair(study, conceptCode)
            }
        } else if (conceptConstraint.path) {
            def concept = conceptsResource.getConceptByConceptPath(conceptConstraint.path)
            result << new StudyConceptPair(study, concept.conceptCode)
        } else {
            return null
        }
        return result
    }

    /**
     * Gets a list of unique study-concept pairs for a concept constraint, which refers to one or more concepts.
     * The list of all study-concept pairs is consulted to retrieve all study-concept pairs for those concepts.
     *
     * @param conceptConstraint the concept constraint
     * @param allStudyConceptPairs the list of all study-concept pairs for the data set.
     * @return the list of unique study-concept pairs for the concepts.
     */
    private List<StudyConceptPair> getStudyAndConceptListForConcept(ConceptConstraint conceptConstraint, List<StudyConceptPair> allStudyConceptPairs) {
        Set<String> conceptCodes
        if (conceptConstraint.conceptCodes) {
            conceptCodes = conceptConstraint.conceptCodes as Set<String>
        } else if (conceptConstraint.conceptCode) {
            conceptCodes = [conceptConstraint.conceptCode] as Set<String>
        } else {
            conceptCodes = [conceptsResource.getConceptCodeByConceptPath(conceptConstraint.path)] as Set<String>
        }
        allStudyConceptPairs.findAll {
            conceptCodes.contains(it.conceptCode)
        }
    }

    /**
     * Gets a list of unique study-concept pairs for a study name constraint.
     * The list of all study-concept pairs is consulted to retrieve all study-concept pairs for those concepts.
     *
     * @param studyNameConstraint the study name constraint
     * @param allStudyConceptPairs the list of all study-concept pairs for the data set.
     * @return the list of unique study-concept pairs for the concepts.
     */
    private List<StudyConceptPair> getStudyAndConceptListForStudy(StudyNameConstraint studyNameConstraint, List<StudyConceptPair> allStudyConceptPairs) {
        allStudyConceptPairs.findAll {
            studyNameConstraint.studyId == it.study.name
        }
    }

    /**
     * Extracts a list of unique study-concept pairs from a query of the shape
     * <code>(study A and (concept 1 or concept 2)) or (concept 3)</code>.
     * If a disjunct contains both a study and a concept restriction, study-concept pairs can be
     * constructed immediately. If a disjunct only contains a concept constraint (cross-study),
     * the list of all study-concept pairs is consulted to retrieve all study-concept pairs with that concept.
     *
     * @param constraint the (disjunctive) column/data constraint.
     * @param allStudyConceptPairs the list of all study-concept pairs for the data set.
     * @return the list of unique study-concept pairs that match the column constraint.
     */
    private List<StudyConceptPair> getStudyAndConceptListFromConstraint(OrConstraint constraint, List<StudyConceptPair> allStudyConceptPairs) {
        // Disjunction of a list of study-concept pairs. Fetch for each disjunct and concatenate.
        List<AndConstraint> andConstraints = []
        List<ConceptConstraint> conceptConstraints = []
        for (Constraint arg: constraint.args) {
            if (arg instanceof Combination && arg.operator == Operator.AND) {
                andConstraints.add(new AndConstraint(arg.args))
            } else if (arg instanceof ConceptConstraint) {
                conceptConstraints.add(arg)
            } else {
                log.debug "Disjunction does not only contain conjunctions."
                return null
            }
        }
        List<StudyConceptPair> result = []
        for (AndConstraint arg: andConstraints) {
            result.addAll(getStudyAndConceptFromConstraint(arg))
        }
        for (ConceptConstraint arg: conceptConstraints) {
            result.addAll(getStudyAndConceptListForConcept(arg, allStudyConceptPairs))
        }
        result.unique()
    }

    /**
     * Get a list of all unique study-concept pairs for a data set based on the constraint.
     * Study-concept pairs with zero counts are excluded from the result.
     * The list is computed using a counts per study and concept query on the database.
     * These counts may be already in the application cache.
     * The query is restricted to the data the current user has access to.
     *
     * @param constraint the constraints that defines the data set.
     * @param user the current user.
     * @return the list of all unique study-concept pairs for the data set.
     */
    private List<StudyConceptPair> getStudyAndConceptListFromCounts(Constraint constraint, User user) {
        // possible workaround: fetch all available study-concept pairs from the counts_per_study_and_concept call.
        log.info "Retrieving column definition from study and concept count ..."
        List<StudyConceptPair> studyConceptPairs = []
        Map<String, Map<String, Counts>> countsPerStudyAndConcept = aggregateDataResource
                .countsPerStudyAndConcept(constraint, user)
        for (Map.Entry<String, Map> entry : countsPerStudyAndConcept.entrySet()) {
            def studyId = entry.key
            def study = studiesResource.getStudyByStudyId(studyId)
            def conceptCodeToCounts = entry.value
            for (Map.Entry<String, Counts> conceptCodeToCountsEntry : conceptCodeToCounts.entrySet()) {
                Counts counts = conceptCodeToCountsEntry.value
                if (counts.patientCount > 0 || counts.observationCount > 0) {
                    String conceptCode = conceptCodeToCountsEntry.key
                    studyConceptPairs << new StudyConceptPair(study, conceptCode)
                }
            }
        }
        studyConceptPairs
    }

    /**
     * Transform a list of study-concept pairs into an immutable list of data columns.
     * @param studyConceptPairs a list of study-concept pairs.
     * @return an immutable list of data columns.
     */
    private static ImmutableList<HypercubeDataColumn> toColumnList(List<StudyConceptPair> studyConceptPairs) {
        ImmutableList.Builder<HypercubeDataColumn> columns = ImmutableList.builder()
        for (StudyConceptPair studyConceptPair: studyConceptPairs) {
            ImmutableMap<Dimension, Object> coordinates = ImmutableMap.builder()
                    .put((Dimension)DimensionImpl.CONCEPT, (Object)studyConceptPair.conceptCode)
                    .put((Dimension)DimensionImpl.STUDY, (Object)studyConceptPair.study.name)
                    .build()
            columns.add(new HypercubeDataColumn(coordinates))
        }
        columns.build()
    }

}
