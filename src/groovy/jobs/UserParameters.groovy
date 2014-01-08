package jobs

import com.google.common.collect.Maps
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

    void each(Closure c) {
        map.each c
    }

}
