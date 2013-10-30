package inc

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
