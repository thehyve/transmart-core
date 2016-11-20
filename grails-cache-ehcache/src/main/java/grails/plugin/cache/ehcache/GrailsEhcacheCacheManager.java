/* Copyright 2012-2013 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.cache.ehcache;

import grails.plugin.cache.GrailsCacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.util.Assert;

/**
 * Based on org.springframework.cache.ehcache.EhCacheCacheManager; reworked
 * to return GrailsEhcacheCache instances.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Burt Beckwith
 * @author Andrew Walters
 */
public class GrailsEhcacheCacheManager implements GrailsCacheManager, InitializingBean {

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected CacheManager cacheManager;
	protected final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>();
	protected Set<String> cacheNames = new LinkedHashSet<String>();

	private final Lock lock = new ReentrantLock();

	public Cache getCache(final String name) {
		Cache cache = cacheMap.get(name);
		if (cache == null) {
			try {
				cache = getOrCreateCache(name);
			}
			catch (InterruptedException e) {
				throw new CacheException("Failed to get lock for " + name + " cache creation");
			}
		}
		return cache;
	}

	protected Cache getOrCreateCache(String name) throws InterruptedException {
		// Ensure we don't have parallel access to cache creation which can lead to 'cache already exists' exceptions
		if (!lock.tryLock(200, TimeUnit.MILLISECONDS)) {
			throw new CacheException("Failed to get lock for " + name + " cache creation");
		}

		try {
			// check the EhCache cache again (in case the cache was added at runtime)
			Ehcache ehcache = cacheManager.getEhcache(name);
			if (ehcache == null) {
				// create a new one based on defaults
				cacheManager.addCache(name);
				ehcache = cacheManager.getEhcache(name);
			}
			Cache cache = new GrailsEhcacheCache(ehcache);
			addCache(cache);
			return cache;
		}
		finally {
			lock.unlock();
		}
	}

	public boolean cacheExists(String name) {
		return getCacheNames().contains(name);
	}

	public boolean destroyCache(String name) {
		cacheManager.removeCache(name);
		cacheMap.remove(name);
		cacheNames.remove(name);
		return true;
	}

	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(cacheNames);
	}

	protected Collection<Cache> loadCaches() {
		Assert.notNull(cacheManager, "A backing EhCache CacheManager is required");
		Status status = cacheManager.getStatus();
		Assert.isTrue(Status.STATUS_ALIVE.equals(status),
				"An 'alive' EhCache CacheManager is required - current cache is " + status);

		String[] names = cacheManager.getCacheNames();
		Collection<Cache> caches = new LinkedHashSet<Cache>(names.length);
		for (String name : names) {
			caches.add(new GrailsEhcacheCache(cacheManager.getEhcache(name)));
		}
		return caches;
	}

	protected void addCache(Cache cache) {
		cacheMap.put(cache.getName(), cache);
		cacheNames.add(cache.getName());
	}

	/**
	 * Set the backing EhCache {@link net.sf.ehcache.CacheManager}.
	 */
	public void setCacheManager(CacheManager manager) {
		cacheManager = manager;
	}

    public CacheManager getUnderlyingCacheManager() {
        return cacheManager;
    }

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(cacheManager, "A backing EhCache CacheManager is required");
		if (Status.STATUS_ALIVE != cacheManager.getStatus()) {
			// loadCaches() will assert on status, so no need to do anything here
			return;
		}

		Collection<? extends Cache> caches = loadCaches();
		// preserve the initial order of the cache names
		for (Cache cache : caches) {
			addCache(cache);
		}

		log.debug("Cache names: {}", getCacheNames());
	}
}
