/* Copyright 2013 SpringSource.
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
package grails.plugin.cache.ehcache

import groovyx.gpars.GParsPool
import net.sf.ehcache.CacheManager
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author Andrew Walters
 */
class EhcacheCacheManagerTests {

	protected GrailsEhcacheCacheManager manager = new GrailsEhcacheCacheManager()

	@Before
	public void setUp() {
		manager.cacheManager = CacheManager.create()
	}

	@After
	public void after() {
		manager.cacheManager.shutdown()
	}

	void testCacheCreationSerialAccess() {
		(0..10).each {
			assert manager.getCache('testCache')
		}
	}

	/**
	 * As a parallel access isn't guaranteed to result in traversing the code at the same time, loop a number
	 * of times to try and cause the simultaneous access to getCache()
	 */
	@Test
	void testCacheCreationParallelAccess() {
		assert !manager.cacheExists('testCache')

		100.times {
			GParsPool.withPool {
				(0..10).everyParallel {
					assert manager.getCache('testCache')
				}

				manager.destroyCache('testCache')
			}
		}
	}

	@Test
	void testCacheGetParallelAccess() {
		manager.getCache('testCache')
		assert manager.cacheExists('testCache')

		100.times {
			GParsPool.withPool {
				(0..100).eachParallel {
					assert manager.getCache('testCache')
				}
			}
		}
	}
}
