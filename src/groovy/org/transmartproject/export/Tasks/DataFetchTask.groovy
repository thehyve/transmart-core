package org.transmartproject.export.Tasks

import au.com.bytecode.opencsv.CSVWriter
import com.google.common.base.Charsets
import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import com.google.common.io.Closer
import groovy.transform.ToString
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.QueriesResource


import static org.transmartproject.core.dataquery.highdim.projections.Projection.DEFAULT_REAL_PROJECTION
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.HIGH_DIMENSIONAL

@Component
@Log4j
class DataFetchTask  {

    private static final char SEPARATOR = "\t" as char


    Map<String /* label prefix */, OntologyTerm> ontologyTerms

    List<Long> resultInstanceIds /* can have nulls */

    Map<String, Map> assayConstraints

    Map<String, Map> dataConstraints

    String projection

    @Autowired
    private HighDimensionResource highDimensionResource

    @Autowired
    private ClinicalDataResource clinicalDataResource

    @Autowired
    private QueriesResource queriesResource

    private class GetTabularResultRunnable {

        String label

        Long resultInstanceId

        OntologyTerm ontologyTerm


        public TabularResult<?, ?> doRun() {
            final Stopwatch stopwatch = Stopwatch.createStarted()

            def res
            if (HIGH_DIMENSIONAL in ontologyTerm.visualAttributes) {
                res = createHighDimensionalResult ontologyTerm, resultInstanceId
            } else {
                res = createClinicalDataResult ontologyTerm, resultInstanceId
            }

            log.info("Fetch for $ontologyTerm (rid $resultInstanceId) " +
                    "finished in $stopwatch")
            res
        }

    }

    public List<File> getTsv() {
        assert resultInstanceIds.size() > 0 && resultInstanceIds.size() <= 2

        /* actualResultInstanceIds removes nulls, but leaves one element with
         * null if the array would be empty otherwise (note that only in dev
         * is it allowed for resultInstanceIds to be == [null]). */
        def actualResultInstanceIds = resultInstanceIds.findAll() ?: [null]
        List<Map> allDatasets =
                [ontologyTerms.entrySet(), actualResultInstanceIds]
                        .combinations()
        def exports = []
        allDatasets.each {
            Map.Entry<String, OntologyTerm> ontologyTermEntry = it[0]
            Long resultInstanceId = it[1]

            String label = ontologyTermEntry.key + '_s' +
                    (resultInstanceIds.indexOf(resultInstanceId) + 1)
            GetTabularResultRunnable runnable = new GetTabularResultRunnable(
                    label: label,
                    resultInstanceId: resultInstanceId,
                    ontologyTerm: ontologyTermEntry.value)
            TabularResult result = runnable.doRun()
            File exportedTsv = writeTabularResult(result)
            exports.add(exportedTsv)
        }
        exports
    }


    private Map<String, Object> packParameters() {
        [
                ontologyTerms    : ontologyTerms,
                resultInstanceIds: resultInstanceIds,
                assayConstraints : assayConstraints,
                dataConstraints  : dataConstraints,
                projection       : projection,
        ]
    }


    private File /* filename */ writeTabularResult(TabularResult<?, ?> tabularResult) {
        String filename = UUID.randomUUID().toString()
        File tmpFile = File.createTempFile(filename, ".txt")
        def fos = new FileOutputStream(tmpFile)
        OutputStream os = new BufferedOutputStream(fos)

        log.info("Will start writing tabular result in file $filename")
        final Stopwatch stopwatch = Stopwatch.createStarted()

        Writer writer = new OutputStreamWriter(os, Charsets.UTF_8)

        def csvWriter = new CSVWriter(writer, SEPARATOR)

        try {
            PeekingIterator<? extends DataRow> it =
                    Iterators.peekingIterator(tabularResult.iterator())
            boolean isBioMarker = false
            try {
                isBioMarker = it.peek().hasProperty('bioMarker')
            } catch (NoSuchElementException e) {
            }
            if (isBioMarker == null) isBioMarker = false
            writeHeader(csvWriter, isBioMarker, tabularResult.indicesList)
            it.each { DataRow row ->
                if (Thread.interrupted()) {
                    throw new InterruptedException(
                            'Thread was interrupted while dumping the ' +
                                    'TabularResult into a file')
                }
                writeLine csvWriter, isBioMarker, row
            }
        } finally {
            csvWriter.close()
        }
        log.info("Finished writing file $filename in $stopwatch")
        tmpFile
    }

    private static void writeHeader(CSVWriter writer,
                                    boolean isBioMarker,
                                    List<? extends DataColumn> columns) {
        List line = ['Row Label']
        if (isBioMarker) {
            line += 'Bio marker'
        }
        line += columns.collect {
            if (it instanceof AssayColumn) {
                it.patient.id  // for high dim data, use patient id rather than row label as the CSV header
            } else {
                it.label
            }
        }

        writer.writeNext(line as String[])
    }

    private static void writeLine(CSVWriter writer, boolean isBioMarker, DataRow row) {
        def rowLabel = row.hasProperty('patient') && row.patient.hasProperty('id') ? row.patient.id : row.label
        List line = [rowLabel]
        if (isBioMarker) {
            line += ((BioMarkerDataRow) row).bioMarker
        }
        row.iterator().each {
            line += it
        }

        writer.writeNext(line as String[])
    }

    private TabularResult<AssayColumn, ?> createHighDimensionalResult(
            OntologyTerm ontologyTerm,
            Long resultInstanceId
    ) {
        def subResource = determineSubResource(ontologyTerm)

        def builtAssayConstraints = [
                subResource.createAssayConstraint(
                        AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                        concept_key: ontologyTerm.key),
        ]
        if (resultInstanceId) {
            builtAssayConstraints << subResource.createAssayConstraint(
                    AssayConstraint.PATIENT_SET_CONSTRAINT,
                    result_instance_id: resultInstanceId)
        }
        if (assayConstraints) {
            builtAssayConstraints +=
                    assayConstraints.collect { k, v ->
                        subResource.createAssayConstraint(v, k)
                    }
        }

        def builtDataConstraints = []
        if (dataConstraints) {
            builtDataConstraints +=
                    dataConstraints.collect { k, v ->
                        subResource.createDataConstraint(v, k)
                    }
        }

        def builtProjection = projection ?
                subResource.createProjection(projection) :
                subResource.createProjection(DEFAULT_REAL_PROJECTION)

        subResource.retrieveData(
                builtAssayConstraints,
                builtDataConstraints,
                builtProjection)
    }

    private HighDimensionDataTypeResource determineSubResource(OntologyTerm term) {
        Map<HighDimensionDataTypeResource, Collection<Assay>> res =
                highDimensionResource.getSubResourcesAssayMultiMap([
                        highDimensionResource.createAssayConstraint(
                                AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                                concept_key: term.key)])

        if (res.keySet().size() > 1) {
            throw new UnexpectedResultException("Found multiple possible " +
                    "data types for ontology term $term: ${res.keySet()}")
        } else if (res.keySet().size() == 0) {
            throw new UnexpectedResultException("No data type found " +
                    "associated with ontology term ${term}.")
        }

        res.keySet().first()
    }

    private TabularResult<ClinicalVariableColumn, PatientRow> createClinicalDataResult(
            OntologyTerm ontologyTerm,
            Long resultInstanceId
    ) {
        if (!resultInstanceId) {
            throw new InvalidArgumentsException(
                    'A result instance id should have been given')
        }

        def clinicalVariable = clinicalDataResource.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_path: ontologyTerm.fullName)


        def queryResult = queriesResource.getQueryResultFromId(resultInstanceId)
        clinicalDataResource.retrieveData(
                queryResult,
                [clinicalVariable])
    }


}
