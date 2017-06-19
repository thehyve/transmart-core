package heim.session

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import groovy.util.logging.Log4j
import org.transmartproject.core.users.User

import java.util.concurrent.atomic.AtomicReference

/**
 * Holds the data for a specific session.
 */
@Log4j
class SessionContext {

    public final static String SMART_R_USER_BEAN = 'smartRBean'

    final UUID sessionId

    final String workflowType

    private final AtomicReference<Date> lastActive = new AtomicReference<>(new Date())

    private Map<String, Object> beans = [:].asSynchronized()

    private Multimap<String, Runnable> destructionCallbacks =
            Multimaps.synchronizedMultimap(HashMultimap.create())


    void updateLastModified() {
        lastActive.set(new Date())
    }

    Date getLastActive() {
        lastActive.get()
    }

    SessionContext(User user, String workflowType) {
        sessionId = UUID.randomUUID()
        this.workflowType = workflowType
        beans[SMART_R_USER_BEAN] = user
    }

    Object getBean(String beanName) {
        beans[beanName]
    }

    void addBean(String beanName, Object value) {
        beans[beanName] = value
    }

    Object removeBean(String beanName) {
        destructionCallbacks.removeAll(beanName)
        beans.remove(beanName)
    }

    void registerDestructionCallback(String name, Runnable callback) {
        destructionCallbacks.put(name, callback)
    }

    void destroy() {
        destructionCallbacks.asMap().each { String bean,
                                            Collection<Runnable> callbacks ->
            log.debug("Calling destruction callbacks for bean $bean")
            callbacks.each {
                it.run()
            }
        }
    }
}
