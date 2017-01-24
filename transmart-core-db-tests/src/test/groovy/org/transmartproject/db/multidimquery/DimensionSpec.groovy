package org.transmartproject.db.multidimquery

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.InvalidArgumentsException
import spock.lang.Ignore
import spock.lang.Specification

import org.transmartproject.db.i2b2data.PatientDimension as I2b2PatientDimension

import static org.transmartproject.db.multidimquery.DimensionImpl.*
import static org.transmartproject.core.multidimquery.Dimension.Size.*
import static org.transmartproject.core.multidimquery.Dimension.Density.*
import static org.transmartproject.core.multidimquery.Dimension.Packable.*

class DimensionSpec extends Specification {

    def 'test dimensions'() {

        expect:
        PATIENT.elemType == Patient
        PATIENT.elementType == null
        PATIENT.elementsSerializable == false
        START_TIME.elementType == Date
        START_TIME.elementsSerializable == true
        PROVIDER.elementType == String
        PROVIDER.elementsSerializable == true

        Date d = new Date(12345)
        START_TIME.asSerializable(d) == d
        PROVIDER.asSerializable("hello world") == "hello world"

        Map properties = [id: null, birthDate: d, deathDate: null, age: 20,
                          race: 'caucasian', maritalStatus: null, sourcesystemCd: 'SS_FOO_CD:1234', sexCd: 'FEMALE',
                        // not in the I2b2PatientDimension object, but in the result
                        inTrialId: "1234", trial: 'SS_FOO_CD', sex: 'female',
        ]
        Patient p = new I2b2PatientDimension(properties)

        ['sexCd', 'sourcesystemCd'].each { properties.remove it }
        (properties.religion = null) ?: 1
        'religion' in properties.keySet()

        def sp = PATIENT.asSerializable(p)
        sp == properties

        // In the current implementation dimensions with serializable elements don't check the element type (for
        // speed) and just return whatever they get passed. However the contract allows this to throw as we are
        // passing an invalid type.
        // PROVIDER.asSerializable(23) == 23


        when:
        PATIENT.asSerializable("hello")

        then:
        thrown(InvalidArgumentsException)

    }


}
