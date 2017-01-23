/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.test

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher;

class Matchers {
    //TODO Print failDetails in fail description
    static class CompareInterfacePropertiesMatcher extends DiagnosingMatcher {

        private Object value
        private Class interf
        private List excludes

        @Override
        boolean matches(Object item, Description description) {
            def interfacePropertyNames = interf.metaClass.properties*.name
            def propertyMap = {
                def ret = it.metaClass.properties.findAll { prop ->
                    interfacePropertyNames.contains(prop.name) &&
                            !excludes.contains(prop.name)
                }.collectEntries { prop ->
                    [ prop.name, prop.getProperty(it) ]
                }

                //TODO Implement better way
                if (DomainClassArtefactHandler.isDomainClass(it.getClass()) &&
                        it.id != null) {
                    ret.id = it.id
                }
                ret
            }

            description.appendText(' was ').appendValue(item)

            if (interf.isAssignableFrom(item.class)) {
                def itemPropertyMap = propertyMap(item)
                def valuePropertyMap = propertyMap(value)

                def diff1 = itemPropertyMap - valuePropertyMap
                if (diff1) {
                    description.appendText(', difference between gotten and expected: ').
                            appendValue(diff1)
                }

                def diff2 = valuePropertyMap - itemPropertyMap
                if (diff2) {
                    description.appendText(', difference between expected and gotten: ')
                            .appendValue(diff2)
                }

                !(diff1 || diff2)
            } else {
                description.appendText  "$interf is not assignable from ${item.class}"
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

    static BaseMatcher hasSameInterfaceProperties(Class interf,
                                                  Object value,
                                                  List excludes = []) {
        new CompareInterfacePropertiesMatcher(
                value: value,
                excludes: excludes,
                interf: interf,
        )
    }

    static BaseMatcher hasEqualValueProperties(Object value, String... properties) {
        return new BaseMatcher<Object>() {
            @Override
            public boolean matches(final Object item) {
                for (int i=1;i<properties.length;i++)
                    if (value[properties[i]] != value[properties[0]])
                        return false;
                return true;
            }
            @Override
            public void describeTo(final Description description) {
                description.appendText("Properties are not equal ");
                for (int i=1;i<properties.length;i++)
                    description.appendValue( properties[i]);
            }
            @Override
            public void describeMismatch(final Object item, final
            Description description) {
                for (int i=1;i<properties.length;i++)
                    if (value[properties[i]] != value[properties[0]])
                        description.appendValue(properties[i]).appendText("was").appendValue(value[properties[i]]);
            }
        }

    }
}
