package org.transmartproject.db.util

import groovy.transform.CompileStatic

import java.util.concurrent.Semaphore

/**
 * Lock that can be shared between threads and can be released
 * by another thread than the thread that locked it.
 * Generally called a mutual exclusion lock or binary semaphore.
 * E.g., when a thread first acquires a lock, then spawns a new thread
 * that should release the lock when it is completed.
 *
 * Example use:
 * <code>
 *     if (!lock.tryLock()) {
 *          throw new RuntimeException('Resource is locked')
 *     }
 *     task {
 *         try {
 *             log.info 'Task started.'
 *             // perform task in task thread
 *         } finally {
 *             lock.unlock()
 *         }
 *     }
 * </code>
 */
@CompileStatic
class SharedLock {

    final private Semaphore semaphore = new Semaphore(1)

    /**
     * Checks if the lock is locked.
     *
     * @return true iff the lock is locked.
     */
    boolean isLocked() {
        semaphore.availablePermits() == 0
    }

    /**
     * Try to acquire the lock. Returns false if the lock is not available,
     * true if the lock is successfully acquired.
     *
     * @return true iff the lock is successfully acquired.
     */
    boolean tryLock() {
        semaphore.tryAcquire()
    }

    /**
     * Release the lock.
     */
    void unlock() {
        semaphore.release()
    }

}
