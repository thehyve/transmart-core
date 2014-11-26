package org.transmartproject.batch.startup

import com.google.common.base.Function
import com.google.common.collect.Maps
import groovy.transform.TypeChecked
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.JobParameters

import java.nio.file.Files
import java.nio.file.Path

/**
 * Base class for classes implementing the reading of '*.params' files, as used
 * in transmart-data.
 */
@TypeChecked
@SuppressWarnings('UnnecessaryConstructor')
abstract class ExternalJobParameters {

    public static final String STUDY_ID = 'STUDY_ID' /* can be a platform name */
    public static final String TOP_NODE = 'TOP_NODE'
    public static final String SECURITY_REQUIRED = 'SECURITY_REQUIRED'

    protected Map<String, String> params = Maps.newHashMap()

    protected ExternalJobParameters() { }

    protected Path filePath

    protected String typeName

    static ExternalJobParameters fromFile(
            Map<String, Class<? extends ExternalJobParameters>> parametersTypeMap,
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

        def instance = parametersTypeMap[typeName].newInstance()
        instance.filePath = filePath
        instance.typeName = typeName

        filePath.eachLine { line ->
            if (line =~ /\A\s+\z/ || line =~ /\A\s*#/) {
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
        instance
    }

    /**
     * Subclasses should implement this to describe the keys that the parameters
     * file can have.
     */
    abstract protected Set<String> getFileParameterKeys()

    String getAt(String index) {
        params[index]
    }

    void putAt(String index, Object value) {
        params[index] = value as String
    }

    @SuppressWarnings('EmptyMethodInAbstractClass')
    void validate() throws InvalidParametersFileException {}

    final void munge() throws InvalidParametersFileException {
        doMunge()

        if (!this[STUDY_ID]) {
            def absolutePath = filePath.toAbsolutePath()
            def count = absolutePath.nameCount
            if (count < 2) {
                throw new InvalidParametersFileException("Could not " +
                        "determine study id from path ${absolutePath}")
            }
            this[STUDY_ID] = absolutePath.subpath(count - 2, count - 1)
        }

        if (this[SECURITY_REQUIRED] == null) {
            this[SECURITY_REQUIRED] = 'N'
        }

        if (!this[TOP_NODE]) {
            def prefix = this[SECURITY_REQUIRED] == 'Y' ?
                    'Private Studies' :
                    'Public Studies'

            this[TOP_NODE] = "\\$prefix\\${this[STUDY_ID]}\\"
        }
    }

    /**
     * May be used by subclasses to change the parameter values into a canonical
     * form and add/remove parameters.
     */
    @SuppressWarnings('EmptyMethodInAbstractClass')
    void doMunge() throws InvalidParametersFileException {}

    final protected Path convertRelativePath(String parameter)
            throws InvalidParametersFileException {
        def fileName = this[parameter]
        if (fileName == null) {
            return
        }

        def absolutePath = filePath.toAbsolutePath()
        def dir = absolutePath.parent.resolve(typeName)
        def file = dir.resolve(fileName)
        if (!Files.isRegularFile(file) ||
                !Files.isReadable(file)) {
            throw new  InvalidParametersFileException(
                    "Parameter $parameter references $fileName, but " +
                            "$file is not regular readable file")
        }

        file
    }

    final protected void mandatory(String parameter) {
        if (this[parameter] == null) {
            throw new  InvalidParametersFileException(
                    "Parameter $parameter mandatory but not defined")
        }
    }

    Object asType(Class type) {
        switch(type) {
            case JobParameters:
                return new JobParameters(Maps.transformValues(params, {
                    new JobParameter((String) it)
                } as Function<String, JobParameter>) )
            case Properties:
                def p = new Properties()
                p.putAll params
                return p
            default:
                super.asType(type)
        }
    }

    abstract Class getJobPath()
}
