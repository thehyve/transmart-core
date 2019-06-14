package org.transmartproject.rest.dataExport

import spock.lang.Specification
import org.transmartproject.rest.dataExport.ExportAsyncJobService


class ExportAsyncJobServiceSpec extends Specification{

    void test_escapeInvalidFileNameChars() {
        expect:
        expectedName == ExportAsyncJobService.escapeInvalidFileNameChars(originalName)

        where:
        originalName            || expectedName
        'name with space'       || 'name with space'
        'name//with/slash'      || 'name__with_slash'
        's?p\\e<c>i:a*l-c|h_a"r'|| 's_p_e_c_i_a_l-c_h_a_r'
        'żźćńąęóµśð~'            || 'żźćńąęóµśð~'
    }

}
