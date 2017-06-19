package heim

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import groovy.transform.TypeChecked
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by glopes on 09-10-2015.
 */
@Component
@TypeChecked
class SmartRExecutorService implements ListeningExecutorService,
        DisposableBean {

    public static final int CORE_POOL_SIZE = 3
    public static final int MAXIMUM_POOL_SIZE = 20
    public static final String SMART_RTHREAD_POOL_NAME = 'smartRThreadPool'
    // TODO: make configurable

    @Delegate
    ListeningExecutorService executorService

    SmartRExecutorService() {
        def pool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                1,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>())
        pool.with {
            threadFactory = new SmartRThreadFactory()
        }
        executorService = MoreExecutors.listeningDecorator(pool)
    }

    void destroy() {
        executorService.shutdownNow()
    }

    static class SmartRThreadFactory implements ThreadFactory {
        private final ThreadGroup group
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        SmartRThreadFactory() {
            SecurityManager s = System.securityManager
            group = s?.threadGroup ?: Thread.currentThread().threadGroup
        }

        public Thread newThread(Runnable r) {
            new Thread(group, r,
                    SMART_RTHREAD_POOL_NAME + '-' +
                            threadNumber.getAndIncrement(),
                    0).with {
                daemon = false
                priority = Thread.NORM_PRIORITY
                (Thread) it
            }
        }
    }

}
