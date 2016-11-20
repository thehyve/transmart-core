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

import grails.plugin.cache.BlockingCache;
import grails.plugin.cache.CacheConfiguration;
import net.sf.ehcache.Ehcache;

/**
 * Ehcache-based implementation of BlockingCache.
 *
 * @author Burt Beckwith
 */
public class EhcacheBlockingCache extends GrailsEhcacheCache implements BlockingCache {

	protected final EhcacheCacheConfiguration configuration;

	public EhcacheBlockingCache(Ehcache ehcache) {
		super(new net.sf.ehcache.constructs.blocking.BlockingCache(ehcache));
		configuration = new EhcacheCacheConfiguration(getBlockingCache().getCacheConfiguration());
	}

	public CacheConfiguration getCacheConfiguration() {
		return configuration;
	}

	public boolean isDisabled() {
		return getBlockingCache().isDisabled();
	}

	public void setTimeoutMillis(int blockingTimeoutMillis) {
		getBlockingCache().setTimeoutMillis(blockingTimeoutMillis);
	}

	protected net.sf.ehcache.constructs.blocking.BlockingCache getBlockingCache() {
		return (net.sf.ehcache.constructs.blocking.BlockingCache)getNativeCache();
	}
}
