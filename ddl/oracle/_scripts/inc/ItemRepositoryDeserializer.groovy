package inc

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
