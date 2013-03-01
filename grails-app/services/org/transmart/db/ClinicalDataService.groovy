package org.transmart.db
import org.transmart.core.data.clinical.*
import org.transmart.core.api.*

class ClinicalDataService implements ClinicalDataAPI {

    def getSamples(Trial trial) {
        return new HashSet<Sample>()
    }
}
