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
package grails.plugin.cache.ehcache

import org.slf4j.Logger
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ByteArrayResource

import grails.plugin.cache.ConfigLoader
import grails.plugin.cache.ehcache.GrailsEhCacheManagerFactoryBean.ReloadableCacheManager

/**
 * @author Burt Beckwith
 */
class EhcacheConfigLoader extends ConfigLoader {

	private final Logger log = LoggerFactory.getLogger('grails.plugin.cache.ehcache.EhcacheConfigLoader')

	protected boolean rebuildable

	public void setRebuildable(boolean rebuildable){
		this.rebuildable = rebuildable
	}

	void reload(List<ConfigObject> configs, ApplicationContext ctx) {
		if(!rebuildable){
			log.info("Reload attempted, but reloading has been disabled by configuration. Ignoring the reload attempt.")
			return
		}

		EhcacheConfigBuilder builder = new EhcacheConfigBuilder()
		for (ConfigObject co : configs) {
			def config = co.config
			if (config instanceof Closure) {
				builder.parse config
			}
		}

		String xml = builder.toXml()
		log.debug "Ehcache generated XML:\n$xml"

		GrailsEhcacheCacheManager cacheManager = ctx.grailsCacheManager

		// make copy of names to avoid CME
		for (String name in ([] + cacheManager.cacheNames)) {
			cacheManager.destroyCache name
		}

		ReloadableCacheManager nativeCacheManager = cacheManager.cacheManager
		nativeCacheManager.rebuild new ByteArrayResource(xml.bytes)

		for (String cacheName in nativeCacheManager.cacheNames) {
			cacheManager.getCache cacheName
		}
	}
}
