package org.transmartproject.batch.beans

import org.springframework.beans.factory.BeanFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.transmartproject.batch.db.TableTruncator

/**
 * A persistent context that can be use to issue truncations.
 * Once opened, the context is never closed. Therefore, the DB connection is
 * not cleanly torn down.
 */
class PersistentContext {

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private static BeanFactory beanFactory =
            new AnnotationConfigApplicationContext(GenericFunctionalTestConfiguration)

    static TableTruncator getTruncator() {
        beanFactory.getBean(TableTruncator)
    }
}
