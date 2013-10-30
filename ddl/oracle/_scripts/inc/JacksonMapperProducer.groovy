package inc

import org.codehaus.jackson.Version
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.SerializationConfig
import org.codehaus.jackson.map.module.SimpleModule

/**
 * Created by glopes on 10/27/13.
 */
class JacksonMapperProducer {
    static ObjectMapper getMapper() {
        def objectMapper = new ObjectMapper()
        SimpleModule oracleDdlModule = new SimpleModule("oracleDdlModule", new Version(1, 0, 0, null))
        oracleDdlModule.addSerializer new ItemRepositorySerializer()
        oracleDdlModule.addDeserializer ItemRepository, new ItemRepositoryDeserializer()
        objectMapper.registerModule oracleDdlModule

        objectMapper.configure SerializationConfig.Feature.INDENT_OUTPUT, true

        objectMapper
    }
}
