package jobs.table.columns

import jobs.table.Column
import org.mapdb.Fun

/**
 * Decorator that transforms values, by using a provided function.
 *
 * Created by carlos on 1/22/14.
 */
class TransformColumnDecorator implements ColumnDecorator {

    @Delegate
    Column inner

    /**
     * Closure closure that should accept a single param of type {@link java.lang.Object} (the original value)
     */
    Closure<Object> valueFunction

    // NOTE: assumes there's no transformer in inner
    Closure<Object> getValueTransformer() {
        { Fun.Tuple3<String, Integer, String> key, Object value ->
            valueFunction(value)
        }
    }

}
