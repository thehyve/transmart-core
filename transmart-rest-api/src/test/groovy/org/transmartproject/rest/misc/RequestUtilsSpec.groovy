package org.transmartproject.rest.misc

import org.transmartproject.core.exceptions.InvalidArgumentsException
import spock.lang.Specification

class RequestUtilsSpec extends Specification{

    void test_parseJson() {
        expect:
        RequestUtils.parseJson('{"hello": "world"}') == [hello: "world"]
        RequestUtils.parseJson(null) == null

        when:
        RequestUtils.parseJson('')

        then:
        thrown InvalidArgumentsException

        when:
        RequestUtils.parseJson('{ "hello": ')

        then:
        thrown InvalidArgumentsException

    }

}
