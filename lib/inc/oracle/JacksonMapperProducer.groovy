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
