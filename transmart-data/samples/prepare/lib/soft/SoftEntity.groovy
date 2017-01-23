/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
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
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

package lib.soft

import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap

@Grab(group='com.google.guava', module='guava', version='15.0')
class SoftEntity {
    File file
    Long startOffset

    String type,
           name

    Multimap cachedHeaderAttributes

    protected getTableBeginAttribute() {
        null
    }

    Multimap getHeaderAttributes() {
        if (cachedHeaderAttributes == null) {
            populateHeaderAttributes()
        }

        cachedHeaderAttributes
    }

    public Object getAt(String attribute) {
        attribute = attribute.toLowerCase()
        boolean multiValued = false
        if (attribute.endsWith('*')) {
            attribute = attribute[0..-2]
            multiValued = true
        }

        Collection values = headerAttributes.get(attribute)
        if (!values) {
            return multiValued ? [] : null
        }
        if (multiValued) {
            return values
        }
        // not multivalued
        if (values.size() != 1) {
            throw new RuntimeException("Attribute $attribute is multi-valued: $values")
        }

        values.iterator().next()
    }

    private populateHeaderAttributes() {
        cachedHeaderAttributes = new LinkedListMultimap()

        file.withInputStream { InputStream is ->
            is.skip startOffset

            def bufferedReader = new BufferedReader(new InputStreamReader(is))
            String lastLine
            while ((lastLine = bufferedReader.readLine()) != null) {
                if (lastLine.length() == 0) {
                    continue
                }
                if (lastLine[0] != '!') {
                    return
                }

                def split = lastLine.split(' = ', 2)

                cachedHeaderAttributes.put(
                        split[0][1..-1].toLowerCase(),
                        split[1] ?: '')
            }
        }
    }

    SoftTable getTable() {
        if (tableBeginAttribute == null) {
            return null
        }

        FileInputStream stream = new FileInputStream(file)
        stream.skip startOffset
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(stream))

        String lastLine
        List<String> headers = []
        while ((lastLine = reader.readLine()) != null) {
            if (lastLine.length() == 0) {
                continue
            }
            if (lastLine[0] == "#") {
                headers << lastLine.split(' = ', 2)[0][1..-1]
            } else if (lastLine.equalsIgnoreCase("!$tableBeginAttribute")) {
                return new SoftTable(reader, headers)
            } else if (lastLine[0] != '!') {
                return null
            }
        }

        null
    }
}
