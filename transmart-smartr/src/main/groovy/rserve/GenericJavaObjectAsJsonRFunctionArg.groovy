package heim.rserve

import groovy.json.JsonBuilder

/**
 * Created by glopes on 09-10-2015.
 */
class GenericJavaObjectAsJsonRFunctionArg implements RFunctionArg {
    String name
    Object object

    @Override
    String asRExpression() {
        def string = new JsonBuilder(object).toString()
        "fromJSON('${RUtil.escapeRStringContent(string)}')"
    }
}
