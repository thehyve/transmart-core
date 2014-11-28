package jobs.misc

import com.google.common.collect.Maps
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by carlos on 2/6/14.
 */
@Component
@Scope('job')
class AnalysisConstraints {

    Map<String, Object> map = Maps.newHashMap()

    Object getAt(String key) {
        map.getAt(key)
    }

    Object getProperty(String propertyName) {
        getAt propertyName
    }

    String toString() {
        "AnalysisConstraints$map"
    }

}
