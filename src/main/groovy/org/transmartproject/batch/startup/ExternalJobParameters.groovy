package org.transmartproject.batch.startup

import com.google.common.base.Function
import com.google.common.collect.Maps
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.JobParameters

import java.nio.file.Files
import java.nio.file.Path

/**
 * Base class for classes implementing the reading of '*.params' files, as used
 * in transmart-data.
 */
@TypeChecked
final class ExternalJobParameters {

    private final Map<String, String> params = Maps.newHashMap()

    private Path filePath

    private String typeName

    private List<? extends ExternalJobParametersModule> modules

    private Set<String> fileParameterKeys

    Class<?> jobPath

    private ExternalJobParameters() {}

    static ExternalJobParameters fromFile(
            Map<String, Class<? extends JobSpecification>> parametersTypeMap,
            Path filePath,
            Map<String, String> overrides = [:]) throws InvalidParametersFileException {

        if (!filePath.toString().endsWith('.params')) {
            throw new InvalidParametersFileException(
                    'Expected the parameters file to end with .params')
        }
        def typeName = filePath.fileName.toString() - ~/\.params$/

        if (!parametersTypeMap[typeName]) {
            throw new InvalidParametersFileException(
                    "The type $typeName is not recognized")
        }

        JobSpecification spec = parametersTypeMap[typeName].newInstance()
        ExternalJobParameters instance = new ExternalJobParameters()
        instance.filePath = filePath
        instance.typeName = typeName
        instance.modules = spec.jobParametersModules
        instance.fileParameterKeys =
                ((List<ExternalJobParametersModule>) spec.jobParametersModules)
                        .collectMany { it.supportedParameters } as Set<String>
        instance.jobPath = spec.jobPath

        filePath.eachLine { line ->
            if (line =~ /\A\s*\z/ || line =~ /\A\s*#/) {
                return // skip comment
            }

            // we're of course not supporting full bash syntax...
            def matcher = line =~ /\A\s*([^=]+)=(['"]|.{0})?(.*?)(?:\2)\s*\z/
            if (!matcher.matches()) {
                throw new InvalidParametersFileException("Could not parse the line '$line'")
            }

            instance.params[matcher.group(1)] = matcher.group(3)
        }

        instance.params.putAll overrides

        instance.validate()
        instance.munge()
        instance.checkForExtraParameters()
        instance
    }

    String getAt(String index) {
        params[index]
    }

    void putAt(String index, Object value) {
        if (value != null) {
            params[index] = value as String
        } else {
            params.remove(index)
        }
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    @SuppressWarnings('UnusedPrivateMethod')
    private void validate() throws InvalidParametersFileException {
        modules.each { it.validate(internalInterface) }
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    @SuppressWarnings('UnusedPrivateMethod')
    private void munge() throws InvalidParametersFileException {
        modules.each { it.munge(internalInterface) }
    }

    @SuppressWarnings('UnusedPrivateMethod')
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
                ExternalJobParameters.this[index]
            }

            void putAt(String index, Object value) {
                ExternalJobParameters.this.putAt(index, value)
            }

            Path getFilePath() {
                ExternalJobParameters.this.filePath
            }

            String getTypeName() {
                ExternalJobParameters.this.typeName
            }

            Path convertRelativePath(String parameter) {
                def fileName = this[parameter]
                if (fileName == null) {
                    return
                }

                def absolutePath = filePath.toAbsolutePath()
                def dir = absolutePath.parent.resolve(typeName)
                def file = dir.resolve(fileName)
                if (!Files.isRegularFile(file) ||
                        !Files.isReadable(file)) {
                    throw new InvalidParametersFileException(
                            "Parameter $parameter references $fileName, but " +
                                    "$file is not regular readable file")
                }

                file
            }

            void mandatory(String parameter) throws InvalidParametersFileException {
                if (this[parameter] == null) {
                    throw new InvalidParametersFileException(
                            "Parameter $parameter mandatory but not defined")
                }
            }

            void mungeBoolean(String parameter, boolean defaultValue) {
                if (this[parameter] == null) {
                    this[parameter] = defaultValue ? 'Y' : 'N'
                } else if (this[parameter] in ['0', 'false', 'N']) {
                    this[parameter] = 'N'
                } else {
                    this[parameter] = 'Y'
                }
            }
        }
    }
}
