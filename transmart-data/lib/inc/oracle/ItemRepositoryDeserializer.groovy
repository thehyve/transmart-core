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

import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonProcessingException
import org.codehaus.jackson.ObjectCodec
import org.codehaus.jackson.map.DeserializationContext
import org.codehaus.jackson.map.JsonDeserializer

import static org.codehaus.jackson.JsonToken.*

class ItemRepositoryDeserializer extends JsonDeserializer<ItemRepository> {
    @Override
    ItemRepository deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        def repository = new ItemRepository()

        ObjectCodec codec = jp.getCodec()

        def assertEquals = { actual, expected ->
            if (actual != expected) {
                throw ctxt.mappingException(ItemRepository)
            }
        }

        assertEquals jp.currentToken, START_OBJECT

        assertEquals jp.nextToken(), FIELD_NAME
        assertEquals jp.text, 'dependencies'

        assertEquals jp.nextToken(), START_ARRAY

        def depsArray = codec.readTree jp
        depsArray.each { JsonNode pair ->
            def child = jsonNodeToMap(pair.get('child'))
            def parents = pair.get('parents').collect { jsonNodeToMap it }
            repository.addItem child
            parents.each { parent ->
                repository.addDependency parent, child
            }
        }

        assertEquals jp.lastClearedToken, END_ARRAY

        assertEquals jp.nextToken(), FIELD_NAME
        assertEquals jp.text, 'fileAssignments'

        assertEquals jp.nextToken(), START_ARRAY

        def fileAssignmentsArray = codec.readTree jp
        fileAssignmentsArray.each { JsonNode pair ->
            def item = jsonNodeToMap(pair.get('item'))
            def file = pair.get('file').textValue
            repository.addFileAssignment item, new File(file)
        }

        assertEquals jp.lastClearedToken, END_ARRAY
        assertEquals jp.nextToken(), END_OBJECT

        assertEquals jp.nextToken(), null //end of stream

        repository
    }

    Item jsonNodeToMap(JsonNode node) {
        new BasicItem(
                type:  node.get('type').textValue,
                owner: node.get('owner').textValue,
                name:  node.get('name').textValue,
        )
    }
}
