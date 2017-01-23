package org.transmartproject.batch.i2b2.variable

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.i2b2.misc.DateConverter

import java.text.SimpleDateFormat

/**
 * Generates the implicit external identifiers for visits and providers.
 */
@Component
@JobScope
class ExternalIdentifierGenerator {

    @Value("#{jobParameters['SOURCE_SYSTEM']}")
    private String sourceSystem

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private String providerIdentifier = "Provider for $sourceSystem"

    @Autowired
    private DateConverter dateConverter

    private final SimpleDateFormat sdf =
            new SimpleDateFormat('YYYY-MM-dd', Locale.ENGLISH)

    String generateVisitExternalIdentifier(
            String startDateString,
            String patientExternalId)
            throws IllegalArgumentException /* if date is bad */ {
        Date date = dateConverter.parse(startDateString)

        String formattedDate
        synchronized (sdf) {
            formattedDate = sdf.format(date)
        }
        "$patientExternalId@$formattedDate"
    }

    String generateProviderIdentifier() {
        providerIdentifier
    }

}
