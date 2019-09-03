package org.transmartproject.api.server.user

import spock.lang.Specification

import java.text.ParseException

import static org.transmartproject.core.users.PatientDataAccessLevel.*

class AccessLevelsSpec extends Specification {

    void 'test parse study token to access level corner cases'() {
        when:
        AccessLevels.parseStudyTokenToAccessLevel(accLvlToTok)
        then:
        def pe = thrown(exception)
        pe.message == message

        where:
        accLvlToTok                  | exception                | message
        '|'                          | ParseException           | "Can't parse permission '${accLvlToTok}'."
        'STUDY1_TOKEN|UNEXISTING_OP' | IllegalArgumentException | 'No enum constant org.transmartproject.core.users.PatientDataAccessLevel.UNEXISTING_OP'
        '|SUMMARY'                   | IllegalArgumentException | "Empty study: '${accLvlToTok}'."
        '|||'                        | ParseException           | "Can't parse permission '${accLvlToTok}'."
        ''                           | ParseException           | "Can't parse permission '${accLvlToTok}'."
    }

    void 'test choose the higher access level in case of collision'() {
        expect:
        result == AccessLevels.buildStudyToPatientDataAccessLevel(roles)

        where:
        roles                                                                     | result
        ['STUDY1|COUNTS_WITH_THRESHOLD', 'STUDY1|SUMMARY']                        | ['STUDY1': SUMMARY]
        ['STUDY1|COUNTS_WITH_THRESHOLD', 'STUDY1|MEASUREMENTS', 'STUDY1|SUMMARY'] | ['STUDY1': MEASUREMENTS]
    }

    void 'test has authorities'() {
        expect:
        AccessLevels.hasAuthorities(['ROLE_ADMIN'])
        AccessLevels.hasAuthorities(['ROLE_PUBLIC'])
        AccessLevels.hasAuthorities(['ROLE_ADMIN', 'ROLE_PUBLIC'])
        AccessLevels.hasAuthorities(['ROLE_ADMIN', 'STUDY1|MEASUREMENTS'])
        AccessLevels.hasAuthorities(['STUDY1|SUMMARY'])
        AccessLevels.hasAuthorities(['ROLE_PUBLIC', 'UNKNOWN_ROLE'])
        !AccessLevels.hasAuthorities([])
        !AccessLevels.hasAuthorities(['UNKNOWN_ROLE'])
        !AccessLevels.hasAuthorities(['STUDY1|UNKNOWN_ROLE'])
    }

}
