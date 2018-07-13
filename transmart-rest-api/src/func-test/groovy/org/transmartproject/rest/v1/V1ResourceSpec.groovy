package org.transmartproject.rest.v1

import org.transmartproject.rest.ResourceSpec

abstract class V1ResourceSpec extends ResourceSpec {

    public static final String VERSION = "v1"

    @Override
    protected String getContextPath() {
        "/${VERSION}"
    }
}
