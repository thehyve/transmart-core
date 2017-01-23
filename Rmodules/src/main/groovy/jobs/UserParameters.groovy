package jobs

import com.google.common.collect.Maps
import groovy.json.JsonBuilder
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('job')
class UserParameters {

    Map<String, Object> map = Maps.newHashMap()

    Object getAt(String key) {
        map.getAt(key)
    }

    /*Object putAt(String key, Object value) {
        map.putAt(key, value)
    }*/

    Object getProperty(String propertyName) {
        getAt propertyName
    }

    String toString() {
        "UserParameters$map"
    }

    String toJSON() {
        JsonBuilder builder = new JsonBuilder()
        builder(new TreeMap(map)) //sorting the map so its easier to compare visually
        return builder.toString()
    }

    void each(Closure c) {
        map.each c
    }

}
