package smartr.rserve

import groovy.json.JsonBuilder

class GenericJavaObjectAsJsonRFunctionArg implements RFunctionArg {
    String name
    Object object

    @Override
    String asRExpression() {
        def string = new JsonBuilder(object).toString()
        "fromJSON('${RUtil.escapeRStringContent(string)}')"
    }
}
