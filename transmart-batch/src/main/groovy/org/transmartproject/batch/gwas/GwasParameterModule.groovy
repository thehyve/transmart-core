package org.transmartproject.batch.gwas

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import org.transmartproject.batch.startup.ExternalJobParametersInternalInterface
import org.transmartproject.batch.startup.ExternalJobParametersModule
import org.transmartproject.batch.startup.InvalidParametersFileException

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Specifies the parameters for the GWAS analysis job.
 */
class GwasParameterModule implements ExternalJobParametersModule {

    public static final String DATA_LOCATION = 'DATA_LOCATION'
    public static final String META_DATA_FILE = 'META_DATA_FILE'
    public static final String HG_VERSION = 'HG_VERSION'

    private static final String DEFAULT_HG_VERSION = '19'
    private static final List<String> ALLOWED_HG_VERSIONS =
            ImmutableList.of('18', '19')

    final Set<String> supportedParameters = ImmutableSet.of(
            DATA_LOCATION,
            META_DATA_FILE,
            HG_VERSION,
    )

    @Override
    void validate(ExternalJobParametersInternalInterface ejp) {
        mandatory ejp, META_DATA_FILE
        if (ejp[HG_VERSION] && !(ejp[HG_VERSION] in ALLOWED_HG_VERSIONS)) {
            throw new InvalidParametersFileException(
                    "HG_VERSION must be one of $ALLOWED_HG_VERSIONS; " +
                            "given ${ejp[HG_VERSION]}")
        }
    }

    @Override
    void munge(ExternalJobParametersInternalInterface ejp) {
        Path metaDataFilePath = convertRelativePath(ejp, META_DATA_FILE)
        ejp[META_DATA_FILE] = metaDataFilePath
        ejp[HG_VERSION] = ejp[HG_VERSION] ?: DEFAULT_HG_VERSION

        if (ejp[DATA_LOCATION] == null) {
            ejp[DATA_LOCATION] = metaDataFilePath.parent
        } else {
            Path dataLocationPath = Paths.get(ejp[DATA_LOCATION])
            if (!dataLocationPath.absolute) {
                dataLocationPath = metaDataFilePath.parent.resolve(
                        dataLocationPath)
            }

            if (!Files.isDirectory(dataLocationPath)) {
                throw new InvalidParametersFileException(
                        "Parameter DATA_LOCATION references " +
                                "$dataLocationPath, which is not a readable " +
                                "directory")
            }
            ejp[DATA_LOCATION] = dataLocationPath.normalize()
        }
    }
}
