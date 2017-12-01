package org.transmartproject.db.util

import groovy.transform.CompileStatic

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Lock that can be shared between threads and can be released
 * by another thread than the thread that locked it.
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

    static class SimpleLock {
        boolean locked
    }

    final private SimpleLock sharedLock = new SimpleLock(locked: false)
    final private Lock lockLock = new ReentrantLock()

    /**
     * Checks if the lock is locked.
     *
     * @return true iff the lock is locked.
     */
    boolean isLocked() {
        sharedLock.locked
    }

    /**
     * Try to acquire the lock. Returns false if the lock is not available,
     * true if the lock is successfully acquired.
     *
     * @return true iff the lock is successfully acquired.
     */
    boolean tryLock() {
        lockLock.lock()
        if (locked) {
            lockLock.unlock()
            return false
        } else {
            sharedLock.locked = true
            lockLock.unlock()
            return true
        }
    }

    /**
     * Release the lock.
     */
    void unlock() {
        lockLock.lock()
        sharedLock.locked = false
        lockLock.unlock()
    }

}
