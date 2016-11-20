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

import net.sf.ehcache.*;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.attribute.DynamicAttributesExtractor;
import net.sf.ehcache.statistics.StatisticsGateway;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Based on org.springframework.cache.ehcache.EhCacheManagerFactoryBean.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Burt Beckwith
 */
public class GrailsEhCacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean, DisposableBean {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected Resource configLocation;
	protected String cacheManagerName;
	protected CacheManager cacheManager;

	protected boolean rebuildable;

	public void setRebuildable(boolean rebuildable){
		this.rebuildable = rebuildable;
	}

	public CacheManager getObject() {
		return cacheManager;
	}

	public Class<? extends CacheManager> getObjectType() {
		return cacheManager == null ? CacheManager.class : cacheManager.getClass();
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() throws IOException, CacheException {
		logger.info("Initializing EHCache CacheManager");

		InputStream inputStream;
		if (configLocation == null) {
			
			if (cacheManagerName == null) {
				logger.info("No custom cacheManagerName given, using default name 'grails-cache-ehcache'.");
				// default name to not have multiple unnamed cache managers (because of hibernate)
				cacheManagerName = "grails-cache-ehcache";
			}
			
			// use dummy configuration for now, will be done for real via rebuild()
			String dummyXml =
					"<ehcache name='" + cacheManagerName + "' updateCheck='false'>" +
					"<defaultCache maxElementsInMemory='1' eternal='false' overflowToDisk='false' timeToLiveSeconds='1234' />" +
					"</ehcache>";
			inputStream = new ByteArrayInputStream(dummyXml.getBytes());
		}
		else {
			inputStream = configLocation.getInputStream();
		}

		try {
			if(rebuildable){
				cacheManager = new ReloadableCacheManager(inputStream);
			}else{
				cacheManager = new CacheManager(inputStream);
			}
		}
		finally {
			inputStream.close();
		}
	}

	public void destroy() {
		logger.info("Shutting down EHCache CacheManager");
		cacheManager.shutdown();
	}

	/**
	 * Set the location of the EHCache config file. A typical value is "/WEB-INF/ehcache.xml".
	 * <p>Default is "ehcache.xml" in the root of the class path, or if not found,
	 * "ehcache-failsafe.xml" in the EHCache jar (default EHCache initialization).
	 * @see net.sf.ehcache.CacheManager#create(java.io.InputStream)
	 * @see net.sf.ehcache.CacheManager#CacheManager(java.io.InputStream)
	 */
	public void setConfigLocation(Resource location) {
		configLocation = location;
	}

	/**
	 * Set the name of the EHCache CacheManager (if a specific name is desired).
	 * @see net.sf.ehcache.CacheManager #setName(String)
	 */
	public void setCacheManagerName(String name) {
		cacheManagerName = name;
	}

	public static class ReloadableCacheManager extends CacheManager {

		public ReloadableCacheManager(InputStream inputStream) {
			super(inputStream);
		}

		public void rebuild(Resource location) throws IOException {

			for (String cacheName : getCacheNames()) {
				removeCache(cacheName);
			}

			for (Object o : getCacheManagerEventListenerRegistry().getRegisteredListeners()) {
				// Listeners must be disposed or there will be problems (ex, ManagementService will register a duplicate mbean, throwing an exception)
				((CacheManagerEventListener)o).dispose();
			}

			status = Status.STATUS_UNINITIALISED;

//            Field diskStorePathManager = ReflectionUtils.findField(CacheManager.class, "diskStorePathManager", net.sf.ehcache.DiskStorePathManager.class);
//			ReflectionUtils.makeAccessible(diskStorePathManager);
//			ReflectionUtils.setField(diskStorePathManager, this, null);

//			@SuppressWarnings("unchecked")
//			Map<CacheManager, String> CACHE_MANAGERS_REVERSE_MAP = (Map<CacheManager, String>)getStaticValue(findField("CACHE_MANAGERS_REVERSE_MAP"));
//			final String name = CACHE_MANAGERS_REVERSE_MAP.remove(this);
//			@SuppressWarnings("unchecked")
//			Map<String, CacheManager> CACHE_MANAGERS_MAP = (Map<String, CacheManager>)getStaticValue(findField("CACHE_MANAGERS_MAP"));
//			CACHE_MANAGERS_MAP.remove(name);

			// remove since it's going to be re-added
//			ALL_CACHE_MANAGERS.remove(this);

			init(null, null, null, location.getInputStream());
		}

		protected Field findField(String name) {
			Field f = ReflectionUtils.findField(getClass(), name);
			ReflectionUtils.makeAccessible(f);
			return f;
		}

		protected Object getStaticValue(Field f) {
			return ReflectionUtils.getField(f, null);
		}

		/** An Ehcache implementation that always gets the cache, by name, from the CacheManager
		 *
		 * Because this CacheManager is reloadable, it may recreate (which is removing then adding) it's underlying caches at any time.
		 * This class ensure that users of the CacheManager always get the cache, and don't get an error saying the cache isn't alive.
		 */
		protected class ProxyEhcache implements Ehcache {
			private final String name;

			public ProxyEhcache(String name){
				this.name = name;
			}

			public Object clone() throws CloneNotSupportedException {
				return getCache(name).clone();
			}

			@Override
			public void put(Element element) throws IllegalArgumentException,
					IllegalStateException, CacheException {
				getUnderlyingEhcache(name).put(element);
			}

			@Override
			public void put(Element element, boolean doNotNotifyCacheReplicators)
					throws IllegalArgumentException, IllegalStateException,
					CacheException {
				getUnderlyingEhcache(name).put(element, doNotNotifyCacheReplicators);
			}

			@Override
			public void putQuiet(Element element)
					throws IllegalArgumentException, IllegalStateException,
					CacheException {
				getUnderlyingEhcache(name).putQuiet(element);
			}

			@Override
			public void putWithWriter(Element element)
					throws IllegalArgumentException, IllegalStateException,
					CacheException {
				getUnderlyingEhcache(name).putWithWriter(element);
			}

			@Override
			public Element putIfAbsent(Element element)
					throws NullPointerException {
				return getUnderlyingEhcache(name).putIfAbsent(element);
			}

			@Override
			public boolean removeElement(Element element)
					throws NullPointerException {
				return getUnderlyingEhcache(name).removeElement(element);
			}

			@Override
			public boolean replace(Element old, Element element)
					throws NullPointerException, IllegalArgumentException {
				return getUnderlyingEhcache(name).replace(old, element);
			}

			@Override
			public Element replace(Element element) throws NullPointerException {
				return getUnderlyingEhcache(name).replace(element);
			}

			@Override
			public Element get(Serializable key) throws IllegalStateException,
					CacheException {
				return getUnderlyingEhcache(name).get(key);
			}

			@Override
			public Element get(Object key) throws IllegalStateException,
					CacheException {
				return getUnderlyingEhcache(name).get(key);
			}

			@Override
			public Element getQuiet(Serializable key)
					throws IllegalStateException, CacheException {
				return getUnderlyingEhcache(name).getQuiet(key);
			}

			@Override
			public Element getQuiet(Object key) throws IllegalStateException,
					CacheException {
				return getUnderlyingEhcache(name).getQuiet(key);
			}

			@Override
			public List getKeys() throws IllegalStateException, CacheException {
				return getUnderlyingEhcache(name).getKeys();
			}

			@Override
			public List getKeysWithExpiryCheck() throws IllegalStateException,
					CacheException {
				return getUnderlyingEhcache(name).getKeysWithExpiryCheck();
			}

			@Override
			public List getKeysNoDuplicateCheck() throws IllegalStateException {
				return getUnderlyingEhcache(name).getKeysNoDuplicateCheck();
			}

			@Override
			public boolean remove(Serializable key)
					throws IllegalStateException {
				return getUnderlyingEhcache(name).remove(key);
			}

			@Override
			public boolean remove(Object key) throws IllegalStateException {
				return getUnderlyingEhcache(name).remove(key);
			}

			@Override
			public boolean remove(Serializable key,
					boolean doNotNotifyCacheReplicators)
					throws IllegalStateException {
				return getUnderlyingEhcache(name).remove(key, doNotNotifyCacheReplicators);
			}

			@Override
			public boolean remove(Object key,
					boolean doNotNotifyCacheReplicators)
					throws IllegalStateException {
				return getUnderlyingEhcache(name).remove(key, doNotNotifyCacheReplicators);
			}

			@Override
			public boolean removeQuiet(Serializable key)
					throws IllegalStateException {
				return getUnderlyingEhcache(name).removeQuiet(key);
			}

			@Override
			public boolean removeQuiet(Object key) throws IllegalStateException {
				return getUnderlyingEhcache(name).removeQuiet(key);
			}

			@Override
			public boolean removeWithWriter(Object key)
					throws IllegalStateException, CacheException {
				return getUnderlyingEhcache(name).removeWithWriter(key);
			}

			@Override
			public void removeAll() throws IllegalStateException,
					CacheException {
				getUnderlyingEhcache(name).removeAll();
			}

			@Override
			public void removeAll(boolean doNotNotifyCacheReplicators)
					throws IllegalStateException, CacheException {
				getUnderlyingEhcache(name).removeAll(doNotNotifyCacheReplicators);
			}

			@Override
			public void flush() throws IllegalStateException, CacheException {
				getUnderlyingEhcache(name).flush();
			}

			@Override
			public int getSize() throws IllegalStateException, CacheException {
				return getUnderlyingEhcache(name).getSize();
			}

			@Override
			public long calculateInMemorySize() throws IllegalStateException,
					CacheException {
				return getUnderlyingEhcache(name).calculateInMemorySize();
			}

			@Override
			public long calculateOffHeapSize() throws IllegalStateException,
					CacheException {
				return getUnderlyingEhcache(name).calculateOffHeapSize();
			}

			@Override
			public long getMemoryStoreSize() throws IllegalStateException {
				return getUnderlyingEhcache(name).getMemoryStoreSize();
			}

			@Override
			public long getOffHeapStoreSize() throws IllegalStateException {
				return getUnderlyingEhcache(name).getOffHeapStoreSize();
			}

			@Override
			public int getDiskStoreSize() throws IllegalStateException {
				return getUnderlyingEhcache(name).getDiskStoreSize();
			}

			@Override
			public Status getStatus() {
				return getUnderlyingEhcache(name).getStatus();
			}

			@Override
			public String getName() {
				return getUnderlyingEhcache(name).getName();
			}

			@Override
			public void setName(String name) {
				getUnderlyingEhcache(name).setName(name);
			}

			@Override
			public boolean isExpired(Element element)
					throws IllegalStateException, NullPointerException {
				return getUnderlyingEhcache(name).isExpired(element);
			}

			@Override
			public RegisteredEventListeners getCacheEventNotificationService() {
				return getUnderlyingEhcache(name).getCacheEventNotificationService();
			}

			@Override
			public boolean isElementInMemory(Serializable key) {
				return getUnderlyingEhcache(name).isElementInMemory(key);
			}

			@Override
			public boolean isElementInMemory(Object key) {
				return getUnderlyingEhcache(name).isElementInMemory(key);
			}

			@Override
			public boolean isElementOnDisk(Serializable key) {
				return getUnderlyingEhcache(name).isElementOnDisk(key);
			}

			@Override
			public boolean isElementOnDisk(Object key) {
				return getUnderlyingEhcache(name).isElementOnDisk(key);
			}

			@Override
			public String getGuid() {
				return getUnderlyingEhcache(name).getGuid();
			}

			@Override
			public CacheManager getCacheManager() {
				return getUnderlyingEhcache(name).getCacheManager();
			}

			@Override
			public void evictExpiredElements() {
				getUnderlyingEhcache(name).evictExpiredElements();
			}

			@Override
			public boolean isKeyInCache(Object key) {
				return getUnderlyingEhcache(name).isKeyInCache(key);
			}

			@Override
			public boolean isValueInCache(Object value) {
				return getUnderlyingEhcache(name).isValueInCache(value);
			}

			@Override
			public void setCacheManager(CacheManager cacheManager) {
				getUnderlyingEhcache(name).setCacheManager(cacheManager);
			}

			@Override
			public BootstrapCacheLoader getBootstrapCacheLoader() {
				return getUnderlyingEhcache(name).getBootstrapCacheLoader();
			}

			@Override
			public void setBootstrapCacheLoader(
					BootstrapCacheLoader bootstrapCacheLoader)
					throws CacheException {
				getUnderlyingEhcache(name).setBootstrapCacheLoader(bootstrapCacheLoader);
			}

			@Override
			public void initialise() {
				getUnderlyingEhcache(name).initialise();
			}

			@Override
			public void bootstrap() {
				getUnderlyingEhcache(name).bootstrap();
			}

			@Override
			public void dispose() throws IllegalStateException {
				getUnderlyingEhcache(name).dispose();
			}

			@Override
			public CacheConfiguration getCacheConfiguration() {
				return getUnderlyingEhcache(name).getCacheConfiguration();
			}

			@Override
			public void registerCacheExtension(CacheExtension cacheExtension) {
				getUnderlyingEhcache(name).registerCacheExtension(cacheExtension);
			}

			@Override
			public void unregisterCacheExtension(CacheExtension cacheExtension) {
				getUnderlyingEhcache(name).unregisterCacheExtension(cacheExtension);
			}

			@Override
			public List<CacheExtension> getRegisteredCacheExtensions() {
				return getUnderlyingEhcache(name).getRegisteredCacheExtensions();
			}

			@Override
			public void setCacheExceptionHandler(
					CacheExceptionHandler cacheExceptionHandler) {
				getUnderlyingEhcache(name).setCacheExceptionHandler(cacheExceptionHandler);
			}

			@Override
			public CacheExceptionHandler getCacheExceptionHandler() {
				return getUnderlyingEhcache(name).getCacheExceptionHandler();
			}

			@Override
			public void registerCacheLoader(CacheLoader cacheLoader) {
				getUnderlyingEhcache(name).registerCacheLoader(cacheLoader);
			}

			@Override
			public void unregisterCacheLoader(CacheLoader cacheLoader) {
				getUnderlyingEhcache(name).unregisterCacheLoader(cacheLoader);
			}

			@Override
			public List<CacheLoader> getRegisteredCacheLoaders() {
				return getUnderlyingEhcache(name).getRegisteredCacheLoaders();
			}

			@Override
			public void registerCacheWriter(CacheWriter cacheWriter) {
				getUnderlyingEhcache(name).registerCacheWriter(cacheWriter);
			}

			@Override
			public void unregisterCacheWriter() {
				getUnderlyingEhcache(name).unregisterCacheWriter();
			}

			@Override
			public CacheWriter getRegisteredCacheWriter() {
				return getUnderlyingEhcache(name).getRegisteredCacheWriter();
			}

			@Override
			public Element getWithLoader(Object key, CacheLoader loader,
					Object loaderArgument) throws CacheException {
				return getUnderlyingEhcache(name).getWithLoader(key, loader, loaderArgument);
			}

			@Override
			public Map getAllWithLoader(Collection keys, Object loaderArgument)
					throws CacheException {
				return getUnderlyingEhcache(name).getAllWithLoader(keys, loaderArgument);
			}

			@Override
			public void load(Object key) throws CacheException {
				getUnderlyingEhcache(name).load(key);
			}

			@Override
			public void loadAll(Collection keys, Object argument)
					throws CacheException {
				getUnderlyingEhcache(name).loadAll(keys, argument);
			}

			@Override
			public boolean isDisabled() {
				return getUnderlyingEhcache(name).isDisabled();
			}

			@Override
			public void setDisabled(boolean disabled) {
				getUnderlyingEhcache(name).setDisabled(disabled);
			}

			@Override
			public Object getInternalContext() {
				return getUnderlyingEhcache(name).getInternalContext();
			}

			@Override
			public void disableDynamicFeatures() {
				getUnderlyingEhcache(name).disableDynamicFeatures();
			}

			@Override
			public CacheWriterManager getWriterManager() {
				return getUnderlyingEhcache(name).getWriterManager();
			}

			@Override
			@Deprecated
			public boolean isClusterCoherent()
					throws TerracottaNotRunningException {
				return getUnderlyingEhcache(name).isClusterCoherent();
			}

			@Override
			@Deprecated
			public boolean isNodeCoherent()
					throws TerracottaNotRunningException {
				return getUnderlyingEhcache(name).isNodeCoherent();
			}

			@Override
			@Deprecated
			public void setNodeCoherent(boolean coherent)
					throws UnsupportedOperationException,
					TerracottaNotRunningException {
				getUnderlyingEhcache(name).setNodeCoherent(coherent);
			}

			@Override
			@Deprecated
			public void waitUntilClusterCoherent()
					throws UnsupportedOperationException,
					TerracottaNotRunningException {
				getUnderlyingEhcache(name).waitUntilClusterCoherent();
			}

			@Override
			public void setTransactionManagerLookup(
					TransactionManagerLookup transactionManagerLookup) {
				getUnderlyingEhcache(name).setTransactionManagerLookup(transactionManagerLookup);
			}

			@Override
			public void addPropertyChangeListener(
					PropertyChangeListener listener) {
				getUnderlyingEhcache(name).addPropertyChangeListener(listener);
			}

			@Override
			public void removePropertyChangeListener(
					PropertyChangeListener listener) {
				getUnderlyingEhcache(name).removePropertyChangeListener(listener);
			}

			@Override
			public <T> Attribute<T> getSearchAttribute(String attributeName)
					throws CacheException {
				return getUnderlyingEhcache(name).getSearchAttribute(attributeName);
			}

			@Override
			public Set<Attribute> getSearchAttributes()
					throws CacheException {
				return getUnderlyingEhcache(name).getSearchAttributes();
			}

			@Override
			public Query createQuery() {
				return getUnderlyingEhcache(name).createQuery();
			}

			@Override
			public boolean isSearchable() {
				return getUnderlyingEhcache(name).isSearchable();
			}

			@Override
			public void acquireReadLockOnKey(Object key) {
				getUnderlyingEhcache(name).acquireReadLockOnKey(key);
			}

			@Override
			public void acquireWriteLockOnKey(Object key) {
				getUnderlyingEhcache(name).acquireWriteLockOnKey(key);
			}

			@Override
			public boolean tryReadLockOnKey(Object key, long timeout)
					throws InterruptedException {
				return getUnderlyingEhcache(name).tryReadLockOnKey(key, timeout);
			}

			@Override
			public boolean tryWriteLockOnKey(Object key, long timeout)
					throws InterruptedException {
				return getUnderlyingEhcache(name).tryReadLockOnKey(key, timeout);
			}

			@Override
			public void releaseReadLockOnKey(Object key) {
				getUnderlyingEhcache(name).releaseReadLockOnKey(key);
			}

			@Override
			public void releaseWriteLockOnKey(Object key) {
				getUnderlyingEhcache(name).releaseWriteLockOnKey(key);
			}

			@Override
			public boolean isReadLockedByCurrentThread(Object key)
					throws UnsupportedOperationException {
				return getUnderlyingEhcache(name).isReadLockedByCurrentThread(key);
			}

			@Override
			public boolean isWriteLockedByCurrentThread(Object key)
					throws UnsupportedOperationException {
				return getUnderlyingEhcache(name).isWriteLockedByCurrentThread(key);
			}

			@Override
			public boolean isClusterBulkLoadEnabled()
					throws UnsupportedOperationException,
					TerracottaNotRunningException {
				return getUnderlyingEhcache(name).isClusterBulkLoadEnabled();
			}

			@Override
			public boolean isNodeBulkLoadEnabled()
					throws UnsupportedOperationException,
					TerracottaNotRunningException {
				return getUnderlyingEhcache(name).isNodeBulkLoadEnabled();
			}

			@Override
			public void setNodeBulkLoadEnabled(boolean enabledBulkLoad)
					throws UnsupportedOperationException,
					TerracottaNotRunningException {
				getUnderlyingEhcache(name).setNodeBulkLoadEnabled(enabledBulkLoad);
			}

			@Override
			public void waitUntilClusterBulkLoadComplete()
					throws UnsupportedOperationException,
					TerracottaNotRunningException {
				getUnderlyingEhcache(name).waitUntilClusterBulkLoadComplete();
			}

			protected Ehcache getUnderlyingEhcache(String name) throws IllegalStateException {
				// We know the cache should exist at this point, so if it doesn't, make it.
				// If it doesn't exist, it's because it did at one point, but because of a rebuild,
				// it was removed, so we should re-add it.
				return ReloadableCacheManager.super.addCacheIfAbsent(name);
			}

			@Override
			public long calculateOnDiskSize() throws IllegalStateException,
					CacheException {
				return getUnderlyingEhcache(name).calculateOnDiskSize();
			}

			@Override
			public Map<Object, Element> getAll(Collection<?> arg0)
					throws IllegalStateException, CacheException,
					NullPointerException {
				return getUnderlyingEhcache(name).getAll(arg0);
			}

			@Override
			public boolean hasAbortedSizeOf() {
				return getUnderlyingEhcache(name).hasAbortedSizeOf();
			}

			@Override
			public void putAll(Collection<Element> arg0)
					throws IllegalArgumentException, IllegalStateException,
					CacheException {
				getUnderlyingEhcache(name).putAll(arg0);
			}

			@Override
			public Element putIfAbsent(Element arg0, boolean arg1)
					throws NullPointerException {
				return getUnderlyingEhcache(name).putIfAbsent(arg0, arg1);
			}

			@Override
			public void registerDynamicAttributesExtractor(
					DynamicAttributesExtractor arg0) {
				getUnderlyingEhcache(name).registerDynamicAttributesExtractor(arg0);
			}

			@Override
			public void removeAll(Collection<?> arg0)
					throws IllegalStateException, NullPointerException {
				getUnderlyingEhcache(name).removeAll(arg0);
			}

			@Override
			public void removeAll(Collection<?> arg0, boolean arg1)
					throws IllegalStateException, NullPointerException {
				getUnderlyingEhcache(name).removeAll(arg0, arg1);
			}

			@Override
			public StatisticsGateway getStatistics()
					throws IllegalStateException {
				return getUnderlyingEhcache(name).getStatistics();
			}
		}

		protected final ConcurrentMap<String, ProxyEhcache> nameToProxyEhcache = new ConcurrentHashMap<String, ProxyEhcache>();

		@Override
		public ProxyEhcache getEhcache(String name) throws IllegalStateException {
			ProxyEhcache ret = nameToProxyEhcache.get(name);
			if(ret!=null){
				// ensure the cache exists. This isn't strictly necessary, but callers may expect to cache to really exist after this call
				addCacheIfAbsent(name);
			}else if(super.getEhcache(name)!=null){
				// it's wasteful to create new ProxyEhcache instances each time a cache is gotten
				// and that wouldn't match the behavior of CacheManager, which returns the same
				// instance for the same name always.
				ProxyEhcache possiblyNewProxyEhcache = new ProxyEhcache(name);
				ret = nameToProxyEhcache.putIfAbsent(name, possiblyNewProxyEhcache);
				if(ret==null){
					//put succeeded, so use the new value
					ret = possiblyNewProxyEhcache;
				}
			}
			return ret;
		}
	}
}
