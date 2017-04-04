/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONElement
import org.grails.web.json.JSONException
import org.grails.web.json.JSONObject
import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher
import org.hamcrest.Matcher

/**
 * Matcher that locates a JSON element (given a path) and delegates matching (to a given matcher) on that element.
 * Json path describes the json fields, separated by '.'.
 * Elements inside an array are defined by [idx].
 * So if we have the following json content ["obj": ["array": [[ "elem": "foo" ], [ "elem": "bar"]]]
 * the path 'obj.array[1].elem' would match the scalar value 'bar'
 * @todo this implementation expects to find array elements by position. Maybe it would be possible to match with a fuzzy/unknown array index
 */
class JsonMatcher extends DiagnosingMatcher<JSONObject> {

    String jsonPath
    Matcher matcher

    public static JsonMatcher matching(String jsonPath, Matcher matcher) {
        new JsonMatcher(jsonPath: jsonPath, matcher: matcher)
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {
        def element = find(jsonPath.split("\\."), 0, item, mismatchDescription)
        if (!element) {
            return false
        }

        if (!matcher.matches(element)) {
            mismatchDescription.appendText(" no match:")
            mismatchDescription.appendText(" expecting ")
            matcher.describeTo(mismatchDescription)
            mismatchDescription.appendText(" was ").appendValue(element)
            return false
        }

        return true
    }

    @Override
    void describeTo(Description description) {
        description.appendText("json element at '$jsonPath' matches ").appendDescriptionOf(matcher)
    }

    private Object find(String[] parts, int index, Object current, Description mismatchDescription) {
        if (index >= parts.length)
            return current

        String name = parts[index]
        String arrayPath
        int arrayPartIndex = name.indexOf('[')
        if (arrayPartIndex >= 0) {
            arrayPath = name.substring(arrayPartIndex)
            name = name.substring(0, arrayPartIndex)
        }

        Object elem
        try {
            elem = getElement(current, name, arrayPath)
        } catch (JSONException ex) {
            mismatchDescription.appendText("element ${parts[index]} not found in $current")
            return null
        }

        find(parts, index + 1, elem, mismatchDescription)
    }

    private Object getElement(JSONElement curr, String name, String arrayPath) {
        if (arrayPath) {
            //finding an element in some JSONArray
            int idx = arrayPath.indexOf(']')
            String part = arrayPath.substring(0, idx + 1)
            int position = part.substring(1, idx) as Integer

            String remainderArrayPath = (arrayPath == part) ? null : arrayPath.substring(part.length())

            JSONArray array
            if (name) {
                //top element is JSONObject
                JSONObject container = curr
                array = container.getJSONArray(name)
            } else {
                //top element is a JSONArray
                array = curr
            }

            JSONElement elem = array.get(position)

            if (remainderArrayPath) {
                //recursing: array inside array
                return getElement(elem, null, remainderArrayPath)
            } else {
                return elem
            }

        } else {
            //finding an element in a JSONObject
            JSONObject container = curr
            return container.get(name)
        }
    }
}
