package org.transmartproject.test

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.hamcrest.BaseMatcher
import org.hamcrest.Description;

class Matchers {
    //TODO Print failDetails in fail description
    static class CompareInterfacePropertiesMatcher extends BaseMatcher {

        private Object value
        private Class interf
        private List excludes
        private def failDetails = []

        @Override
        boolean matches(Object item) {
            def interfacePropertyNames = interf.metaClass.properties*.name
            def propertyMap = {
                def ret = it.properties.findAll {k, v ->
                    interfacePropertyNames.contains(k) &&
                            !excludes.contains(k)
                }
                //TODO Implement better way
                if (DomainClassArtefactHandler.isDomainClass(it.getClass()) &&
                        it.id != null) {
                    ret.id = it.id
                }
                ret
            }

            if (interf.isAssignableFrom(item.class)) {
                def itemPropertyMap = propertyMap(item)
                def valuePropertyMap = propertyMap(value)
                failDetails << value.properties
                def diff1 = itemPropertyMap - valuePropertyMap
                if (diff1) {
                    failDetails << "$item has different values than $value:\n$diff1"
                }

                def diff2 = valuePropertyMap - itemPropertyMap
                if (diff2) {
                    failDetails << "$value has different values than $item:\n$diff2"
                }

                !(diff1 || diff2)
            } else {
                failDetails << "$interf is not assignable from ${item.class}"
                false
            }
        }

        @Override
        void describeTo(Description description) {
            description.appendText("hasSameInterfaceProperties(")
                    .appendValue(interf.name).appendText(", ")
                    .appendValue(value.toString()).appendText(", ")
                    .appendValue(excludes.toString()).appendText(")")
        }
    }

    private static BaseMatcher hasSameInterfaceProperties(Class interf,
                                                          Object value,
                                                          List excludes = []) {
        new CompareInterfacePropertiesMatcher(
                value: value,
                excludes: excludes,
                interf: interf,
        )
    }
}
