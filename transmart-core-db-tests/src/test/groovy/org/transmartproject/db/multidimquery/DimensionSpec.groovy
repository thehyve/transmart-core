package org.transmartproject.db.multidimquery

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.InvalidArgumentsException
import spock.lang.Specification

import org.transmartproject.db.i2b2data.PatientDimension as I2b2PatientDimension

import static org.transmartproject.db.multidimquery.DimensionImpl.*

class DimensionSpec extends Specification {

    def foo(Map map) {
        map.put('religion', 42)
        map.put('religion', null)
    }

    def 'test dimensions'() {

        expect:
        PATIENT.elementType == Patient
        PATIENT.elementTypeIsSerializable == false
        START_TIME.elementType == Date
        START_TIME.elementTypeIsSerializable == true
        PROVIDER.elementType == String
        PROVIDER.elementTypeIsSerializable == true

        Date d = new Date(12345)
        START_TIME.asSerializable(d) == d
        PROVIDER.asSerializable("hello world") == "hello world"

        Map properties = [inTrialId: "1234", birthDate: d, deathDate: null, age: 20,
                          race: 'caucasian', maritalStatus: null, sourcesystemCd: 'SS_FOO_CD:1234', sexCd: 'FEMALE']
        Patient p = new I2b2PatientDimension(properties)
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
