package org.transmartproject.batch.startup

import com.google.common.base.Function
import com.google.common.collect.Maps
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.JobParameters
import org.transmartproject.batch.backout.BackoutJobSpecification
import org.transmartproject.batch.clinical.ClinicalJobSpecification
import org.transmartproject.batch.gwas.GwasJobSpecification
import org.transmartproject.batch.highdim.cnv.data.CnvDataJobSpecification
import org.transmartproject.batch.highdim.cnv.platform.CnvAnnotationJobSpecification
import org.transmartproject.batch.highdim.metabolomics.data.MetabolomicsDataJobSpecification
import org.transmartproject.batch.highdim.metabolomics.platform.MetabolomicsAnnotationJobSpecification
import org.transmartproject.batch.highdim.mirna.data.MirnaDataJobSpecification
import org.transmartproject.batch.highdim.mirna.platform.MirnaAnnotationJobSpecification
import org.transmartproject.batch.highdim.mrna.data.MrnaDataJobSpecification
import org.transmartproject.batch.highdim.mrna.platform.AnnotationJobSpecification
import org.transmartproject.batch.highdim.mrna.platform.MrnaAnnotationJobSpecification
import org.transmartproject.batch.highdim.proteomics.data.ProteomicsDataJobSpecification
import org.transmartproject.batch.highdim.proteomics.platform.ProteomicsAnnotationJobSpecification
import org.transmartproject.batch.highdim.rnaseq.data.RnaSeqDataJobSpecification
import org.transmartproject.batch.highdim.rnaseq.platform.RnaSeqAnnotationJobSpecification
import org.transmartproject.batch.i2b2.I2b2JobSpecification
import org.transmartproject.batch.support.StringUtils
import org.transmartproject.batch.tag.TagsLoadJobSpecification

import java.nio.file.Files
import java.nio.file.Path

/**
 * Gets all details needed for running a job.
 */
@TypeChecked
@Slf4j
final class JobStartupDetails {

    private final static Map<String, Class<? extends JobSpecification>> DATA_TYPE_TO_JOB_SPEC = [
            'clinical'               : ClinicalJobSpecification,
            //Legacy. Deprecated. Use mrna_annotation instead
            'annotation'             : AnnotationJobSpecification,
            //New name for mrna platform. Unlike old platform data file, new file contains a header.
            //That's why we need ne specification class.
            'mrna_annotation'        : MrnaAnnotationJobSpecification,
            'tags'                   : TagsLoadJobSpecification,
            'expression'             : MrnaDataJobSpecification,
            'metabolomics_annotation': MetabolomicsAnnotationJobSpecification,
            'metabolomics'           : MetabolomicsDataJobSpecification,
            'i2b2'                   : I2b2JobSpecification,
            'proteomics_annotation'  : ProteomicsAnnotationJobSpecification,
            'proteomics'             : ProteomicsDataJobSpecification,
            'gwas'                   : GwasJobSpecification,
            'backout'                : BackoutJobSpecification,
            'rnaseq_annotation'      : RnaSeqAnnotationJobSpecification,
            'rnaseq'                 : RnaSeqDataJobSpecification,
            'cnv_annotation'         : CnvAnnotationJobSpecification,
            'cnv'                    : CnvDataJobSpecification,
            'mirna_annotation'       : MirnaAnnotationJobSpecification,
            'mirna'                  : MirnaDataJobSpecification,
    ]
    public static final String STUDY_PARAMS_FILE_NAME = 'study' + PARAMS_FILE_EXTENSION
    public static final String PARAMS_FILE_EXTENSION = '.params'

    private Map<String, String> params

    private Path dataTypeParamsFilePath

    private String typeName

    private List<? extends ExternalJobParametersModule> modules

    private Set<String> fileParameterKeys

    Class<?> jobPath

    private JobStartupDetails() {}

    /**
     * Detects the job to run by '*.params' file.
     * @param filePath - a *.params file
     * @param overrides - parameters to override ones in the *.params file
     * @return The job details.
     */
    static JobStartupDetails fromFile(Path filePath, Map<String, String> overrides = [:]) {

        String dataType = getDataTypeByProperyFile(filePath)
        JobSpecification spec = getJobSpecificationByDataType(dataType)

        JobStartupDetails instance = new JobStartupDetails()
        instance.with {
            dataTypeParamsFilePath = filePath
            typeName = dataType
            modules = spec.jobParametersModules
            fileParameterKeys = getFileParameterKeys(spec)
            jobPath = spec.jobPath
            params = loadParams(filePath, overrides, spec)

            munge()
        }

        instance
    }

    private static Map<String, String> loadParams(
            Path filePath, Map<String, String> overrides, JobSpecification spec) {
        Map<String, String> params = Maps.newHashMap()
        Map<String, String> dataTypeParams = parseContent(filePath)
        if (isStudyRelatedSpecification(spec)) {
            Path studyParamsFilePath = findStudyFilePath(filePath)
            if (studyParamsFilePath) {
                Map<String, String> studyParams = parseContent(studyParamsFilePath)
                dataTypeParams.each { item ->
                    if (studyParams.containsKey(item.key)) {
                        throw new InvalidParametersFileException(
                                "Study-wide ${item.key} parameter gets overriden by data type one:" +
                                        " ${studyParams[item.value]} -> ${item.value}")
                    }
                }
                params << studyParams
            }
        }
        params << dataTypeParams
        params << overrides
        params
    }

    private static Set<String> getFileParameterKeys(JobSpecification spec) {
        ((List<ExternalJobParametersModule>) spec.jobParametersModules)
                .collectMany { it.supportedParameters } as Set<String>
    }

    private static JobSpecification getJobSpecificationByDataType(String typeName) {
        if (!DATA_TYPE_TO_JOB_SPEC[typeName]) {
            throw new InvalidParametersFileException(
                    "The type $typeName is not recognized")
        }

        DATA_TYPE_TO_JOB_SPEC[typeName].newInstance()
    }

    private static String getDataTypeByProperyFile(Path filePath) {
        String fileNameStr = filePath.fileName.toString()
        if (!fileNameStr.endsWith(PARAMS_FILE_EXTENSION)) {
            throw new InvalidParametersFileException(
                    "Expected the parameters file to end with ${PARAMS_FILE_EXTENSION}")
        }

        StringUtils.removeLast(fileNameStr, PARAMS_FILE_EXTENSION)
    }

    private static boolean isStudyRelatedSpecification(JobSpecification spec) {
        spec.jobParametersModules.any { it instanceof StudyJobParametersModule }
    }

    private static Map<String, String> parseContent(Path filePath) {
        Map<String, String> result = [:]
        filePath.eachLine { line ->
            if (line =~ /\A\s*\z/ || line =~ /\A\s*#/) {
                return // skip comment
            }

            // we're of course not supporting full bash syntax...
            def matcher = line =~ /\A\s*([^=]+)=(['"]|.{0})?(.*?)(?:\2)\s*\z/
            if (!matcher.matches()) {
                throw new InvalidParametersFileException("Could not parse the line '$line'")
            }

            result[matcher.group(1)] = matcher.group(3)
        }
        result
    }

    private static Path findStudyFilePath(Path child) {
        Path result = child.toAbsolutePath()
        while (result && !Files.isRegularFile(result.resolve(STUDY_PARAMS_FILE_NAME))) {
            log.debug("Seeking study.params in ${result}")
            result = result.parent
        }
        result?.resolve(STUDY_PARAMS_FILE_NAME)
    }

    JobStartupDetails plus(JobStartupDetails externalJobParameters) {
        params.putAll(externalJobParameters.params)
        this
    }

    String getAt(String key) {
        params[key]
    }

    void putAt(String index, Object value) {
        if (value != null) {
            params[index] = value as String
        } else {
            params.remove(index)
        }
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    private void munge() throws InvalidParametersFileException {
        checkForEmptyValues()

        modules.each { it.munge(internalInterface) }

        validate()
        checkForExtraParameters()
    }

    private void checkForEmptyValues() throws InvalidParametersFileException {
        def emptyValueParams = params.findAll { !it.value }
        if (emptyValueParams) {
            throw new InvalidParametersFileException("Following parameters are specified without a value: " +
                    emptyValueParams.keySet().join(', ') +
                    " Please provide a value or remove parameter.")
        }
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    private void validate() throws InvalidParametersFileException {
        modules.each { it.validate(internalInterface) }
    }

    private void checkForExtraParameters() throws InvalidParametersFileException {
        params.keySet().each { String paramName ->
            if (!fileParameterKeys.contains(paramName)) {
                throw new InvalidParametersFileException(
                        "Parameter '$paramName' not recognized.")
            }
        }
    }

    Object asType(Class type) {
        switch (type) {
            case JobParameters:
                return new JobParameters(Maps.transformValues(params, {
                    new JobParameter((String) it)
                } as Function<String, JobParameter>))
            case Properties:
                def p = new Properties()
                p.putAll params
                return p
            default:
                super.asType(type)
        }
    }

    private ExternalJobParametersInternalInterface getInternalInterface() {
        new ExternalJobParametersInternalInterface() {
            String getAt(String index) {
                JobStartupDetails.this[index]
            }

            void putAt(String index, Object value) {
                JobStartupDetails.this.putAt(index, value)
            }

            Path getFilePath() {
                dataTypeParamsFilePath
            }

            String getTypeName() {
                JobStartupDetails.this.typeName
            }
        }
    }
}
