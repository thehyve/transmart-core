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

package inc.oracle

import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.JsonProcessingException
import org.codehaus.jackson.map.JsonSerializer
import org.codehaus.jackson.map.SerializerProvider

class ItemRepositorySerializer extends JsonSerializer<ItemRepository> {
    @Override
    void serialize(ItemRepository value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject()

        /* we enforce a specific sorting in order to minimize diffs
         * upon re-dumping */
        jgen.writeArrayFieldStart 'dependencies'
        asSorted(value.dependencies).each { Map.Entry<Item, Set<Item>> entry ->
            jgen.writeObject child: entry.key, parents: asSorted(entry.value)
        }
        jgen.writeEndArray()

        jgen.writeArrayFieldStart 'fileAssignments'
        asSorted(value.fileAssignments).each { Map.Entry<Item, String> entry ->
            jgen.writeObject item: entry.key, file: entry.value
        }
        jgen.writeEndArray()

        jgen.writeEndObject()
    }

    @Override
    Class<ItemRepository> handledType() {
        ItemRepository
    }

    private asSorted(Map map) {
        new TreeMap(map)
    }

    private asSorted(Set set) {
        new TreeSet(set)
    }
}
