package org.transmartproject.rest.v2

import org.transmartproject.rest.ResourceSpec

abstract class V2ResourceSpec extends ResourceSpec {

    public static final String VERSION = "v2"

    @Override
    protected String getContextPath() {
        "/${VERSION}"
    }
}
