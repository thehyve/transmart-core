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

import java.util.Collection;

import grails.plugin.cache.GrailsCache;
import grails.plugin.cache.GrailsValueWrapper;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.springframework.cache.ehcache.EhCacheCache;

/**
 * Extends the default implementation to return GrailsValueWrapper instances instead of
 * SimpleValueWrapper, to include the Ehcache Element instance.
 *
 * @author Burt Beckwith
 */
public class GrailsEhcacheCache extends EhCacheCache implements GrailsCache {

	public GrailsEhcacheCache(Ehcache ehcache) {
		super(ehcache);
	}

	@Override
	public GrailsValueWrapper get(Object key) {
		Element element = getNativeCache().get(key);
		return element == null ? null : new GrailsValueWrapper(element.getObjectValue(), element);
	}

	@SuppressWarnings("unchecked")
	public Collection<Object> getAllKeys() {
		return getNativeCache().getKeys();
	}
}
