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

import grails.util.Environment

import org.slf4j.Logger
import org.slf4j.LoggerFactory

// note 'defaults' doesn't apply to hibernateQuery/hibernateTimestamps

/**
 * @author Burt Beckwith
 * @author Dan Stieglitz
 */
class EhcacheConfigBuilder extends BuilderSupport {

	protected static final String INDENT = '\t'
	protected static final String LF = System.getProperty('line.separator')

	protected static final Map<String, Integer> TTL = [
		host: 0,
		subnet: 1,
		site: 32,
		region: 64,
		continent: 128,
		unrestricted: 255]

	protected static final Map<String, String> CACHE_MANAGER_PEER_PROVIDERS =
		[rmi:     'net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory',
		 jgroups: 'net.sf.ehcache.distribution.jgroups.JGroupsCacheManagerPeerProviderFactory',
		 jms:     'net.sf.ehcache.distribution.jms.JMSCacheManagerPeerProviderFactory']

	protected static final Map<String, String> CACHE_EVENT_LISTENER_FACTORIES =
		[rmi:     'net.sf.ehcache.distribution.RMICacheReplicatorFactory',
		 jgroups: 'net.sf.ehcache.distribution.jgroups.JGroupsCacheReplicatorFactory',
		 jms:     'net.sf.ehcache.distribution.jms.JMSCacheReplicatorFactory']

	protected static final Map<String, String> BOOTSTRAP_CACHE_LOADER_FACTORIES =
		[rmi:     'net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory',
		 jgroups: 'net.sf.ehcache.distribution.jgroups.JGroupsBootstrapCacheLoaderFactory'] // no JMS

	protected static final Map DEFAULT_DEFAULT_CACHE = [
		maxElementsInMemory: 10000,
		eternal: false,
		timeToIdleSeconds: 120,
		timeToLiveSeconds: 120,
		overflowToDisk: true,
		maxElementsOnDisk: 10000000,
		diskPersistent: false,
		diskExpiryThreadIntervalSeconds: 120,
		memoryStoreEvictionPolicy: 'LRU']

	protected static final List<String> CACHE_NONPROPERTY_NAMES = [
		'name', 'domain', 'cacheEventListenerFactoryName',
		'bootstrapCacheLoaderFactoryName', 'cacheExceptionHandlerFactoryName',
		'cacheLoaderFactoryName', 'cacheExtensionFactoryName']

	protected List<String> stack = []
	protected List<Map<String, Object>> caches = []
	protected Map<String, Object> defaultCache
	protected Map<String, Object> defaults = [:]
	protected Map<String, Object> hibernateQuery = [
		name: 'org.hibernate.cache.StandardQueryCache', maxElementsInMemory: 50,
		timeToLiveSeconds: 120, eternal: false, overflowToDisk: true, maxElementsOnDisk: 0]
	protected Map<String, Object> hibernateTimestamps = [
		name: 'org.hibernate.cache.UpdateTimestampsCache', maxElementsInMemory: 5000,
		eternal: true, overflowToDisk: false, maxElementsOnDisk: 0]
	protected Map<String, Object> cacheManagerPeerListenerFactory // can be empty, so exists == not null
	protected Map<String, Object> cacheManagerEventListenerFactory = [:]
	protected Map<String, Object> bootstrapCacheLoaderFactory = [:]
	protected Map<String, Object> cacheExceptionHandlerFactory = [:]
	protected Map<String, Object> provider = [:]
	protected List<Map<String, Object>> cacheEventListenerFactories = []
	protected List<Map<String, Object>> cacheLoaderFactories = []
	protected List<Map<String, Object>> cacheExtensionFactories = []
	protected List<Map<String, Object>> cacheManagerPeerProviderFactories = []
	protected Map<String, Object> current
	protected String diskStore = TEMP_DIR
	protected int unrecognizedElementDepth = 0
    protected Map<String, Object> sizeOfPolicy = null

	protected final Logger log = LoggerFactory.getLogger(getClass())

	protected static final List DEFAULT_CACHE_PARAM_NAMES = [
		'cacheLoaderTimeoutMillis', 'clearOnFlush', 'copyOnRead', 'copyOnWrite',
		'diskAccessStripes', 'diskExpiryThreadIntervalSeconds', 'diskSpoolBufferSizeMB',
		'diskPersistent', 'eternal', 'maxElementsInMemory', 'maxElementsOnDisk',
		'maxEntriesLocalDisk', 'maxEntriesLocalHeap', 'maxMemoryOffHeap',
		'memoryStoreEvictionPolicy', 'overflowToDisk', 'overflowToOffHeap',
		'statistics', 'timeToIdleSeconds', 'timeToLiveSeconds', 'transactionalMode']

	protected static final List CACHE_PARAM_NAMES = DEFAULT_CACHE_PARAM_NAMES + [
		'env', 'logging', 'maxBytesLocalDisk', 'maxBytesLocalHeap', 'maxBytesLocalOffHeap', 'name']

	protected static final List RMI_CACHE_MANAGER_PARAM_NAMES = [
		'env', 'factoryType', 'multicastGroupAddress', 'multicastGroupPort',
		'timeToLive', 'className', 'rmiUrl', 'hostName']

	protected static final List JGROUPS_CACHE_MANAGER_PARAM_NAMES = ['env', 'connect']

	protected static final List JMS_CACHE_MANAGER_PARAM_NAMES = [
		'env', 'initialContextFactoryName', 'providerURL', 'topicConnectionFactoryBindingName',
		'topicBindingName', 'getQueueBindingName', 'securityPrincipalName', 'securityCredentials',
		'urlPkgPrefixes', 'userName', 'password', 'acknowledgementMode']

	protected static final List FACTORY_REF_NAMES = [
		'cacheEventListenerFactoryName', 'bootstrapCacheLoaderFactoryName', 'cacheExceptionHandlerFactoryName',
		'cacheLoaderFactoryName', 'cacheExtensionFactoryName']

	protected static final List PROVIDER_NAMES = [
		'updateCheck', 'monitoring', 'dynamicConfig', 'name', 'defaultTransactionTimeoutInSeconds',
		'maxBytesLocalHeap', 'maxBytesLocalOffHeap', 'maxBytesLocalDisk']

	protected static final String TEMP_DIR = 'java.io.tmpdir'

    protected static final List SIZE_OF_POLICY_PARAM_NAMES = ['maxDepth', 'maxDepthExceededBehavior']

	/**
	 * Convenience method to parse a config closure.
	 * @param c the closure
	 */
	void parse(Closure c) {
		c.delegate = this
		c.resolveStrategy = Closure.DELEGATE_FIRST
		c()

		resolveProperties()
	}

	@Override
	protected createNode(name) {
		if (unrecognizedElementDepth) {
			unrecognizedElementDepth++
			log.warn "ignoring node $name contained in unrecognized parent node"
			return
		}

		log.trace "createNode $name"

		switch (name) {
			case 'provider':
			case 'diskStore':
			case 'defaults':
			case 'cacheManagerEventListenerFactory':
			case 'bootstrapCacheLoaderFactory':
			case 'cacheExceptionHandlerFactory':
				stack.push name
				return name

            case 'sizeOfPolicy':
                if (sizeOfPolicy == null) {
                    sizeOfPolicy = [:]
                }
                stack.push name
                return name

			case 'defaultCache':
				if (defaultCache == null) {
					defaultCache = [:]
				}
				stack.push name
				return name

			case 'cacheManagerPeerListenerFactory':
				if (cacheManagerPeerListenerFactory == null) {
					cacheManagerPeerListenerFactory = [:]
				}
				stack.push name
				return name

			case 'hibernateQuery':
				stack.push name
				caches << hibernateQuery.clone()
				return name

			case 'hibernateTimestamps':
				stack.push name
				caches << hibernateTimestamps.clone()
				return name

			case 'domainCollection':
			case 'collection':
			case 'cache':
			case 'domain':
				current = [:]
				caches << current
				stack.push name
				return name

			case 'cacheManagerPeerProviderFactory':
				current = [:]
				cacheManagerPeerProviderFactories << current
				stack.push name
				return name

			case 'cacheEventListenerFactory':
				current = [:]
				cacheEventListenerFactories << current
				stack.push name
				return name

			case 'cacheLoaderFactory':
				current = [:]
				cacheLoaderFactories << current
				stack.push name
				return name

			case 'cacheExtensionFactory':
				current = [:]
				cacheExtensionFactories << current
				stack.push name
				return name
		}

		unrecognizedElementDepth++
		log.warn "Cannot create empty node with name '$name'"
	}

	@Override
	protected createNode(name, value) {
		if (unrecognizedElementDepth) {
			unrecognizedElementDepth++
			log.warn "ignoring node $name with value $value contained in unrecognized parent node"
			return
		}

		log.trace "createNode $name, value: $value"

		String level = stack[-1]
		stack.push name

		switch (level) {
            case 'sizeOfPolicy':
                if (name in SIZE_OF_POLICY_PARAM_NAMES) {
                    sizeOfPolicy[name] = value
                    return name
                }
                break
			case 'diskStore':
				switch (name) {
					case 'path':
						diskStore = value.toString()
						return name

					case 'temp':
						diskStore = TEMP_DIR
						return name

					case 'home':
						diskStore = 'user.home'
						return name

					case 'current':
						diskStore = 'user.dir'
						return name
				}
				break

			case 'defaultCache':
				if (name in DEFAULT_CACHE_PARAM_NAMES) {
					defaultCache[name] = value
					return name
				}
				break

			case 'defaults':
				if (name in FACTORY_REF_NAMES) {
					addToList defaults, name, value
					return name
				}

				if (name in CACHE_PARAM_NAMES) {
					defaults[name] = value
					return name
				}

				break

			case 'hibernateQuery':
				if (name in FACTORY_REF_NAMES) {
					addToList hibernateQuery, name, value
					return name
				}

				if (name in CACHE_PARAM_NAMES) {
					hibernateQuery[name] = value
					return name
				}

				break

			case 'hibernateTimestamps':
				if (name in FACTORY_REF_NAMES) {
					addToList hibernateTimestamps, name, value
					return name
				}

				if (name in CACHE_PARAM_NAMES) {
					hibernateTimestamps[name] = value
					return name
				}
				break

			case 'domain':
			case 'cache':
			case 'domainCollection':
				if (('name' == name || 'cache' == name || 'domain' == name) && value instanceof Class) {
					value = value.name
				}

				if ('name' == name || 'cache' == name  || 'domain' == name || name in CACHE_PARAM_NAMES) {
					current[name] = value
					return name
				}

				if (name in FACTORY_REF_NAMES) {
					addToList current, name, value
					return name
				}

				break

			case 'collection':
				if ('name' == name) {
					if (value instanceof Class) {
						value = value.name
					}

					current.domain = caches[-2].name
				}

				if ('name' == name || name in CACHE_PARAM_NAMES) {
					current[name] = value
					return name
				}

				break

			case 'cacheManagerPeerProviderFactory':
				if ('rmiUrl' == name) {
					addToList current, 'rmiUrls', value
					return name
				}

				// allow all properties for forward compatability
				current[name] = value
				return name

			case 'cacheManagerPeerListenerFactory':
				// allow all properties for forward compatability
				cacheManagerPeerListenerFactory[name] = value
				return name

			case 'cacheManagerEventListenerFactory':
				// allow all properties for forward compatability
				cacheManagerEventListenerFactory[name] = value
				return name

			case 'bootstrapCacheLoaderFactory':
				// allow all properties for forward compatability
				bootstrapCacheLoaderFactory[name] = value
				return name

			case 'provider':
				if (name in PROVIDER_NAMES) {
					provider[name] = value
					return name
				}
				break

			case 'cacheEventListenerFactory':
			case 'cacheLoaderFactory':
			case 'cacheExtensionFactory':
				// allow all properties for forward compatability
				current[name] = value
				return name
		}

		unrecognizedElementDepth++
		log.warn "Cannot create node with name '$name' and value '$value' for parent '$level'"
	}

	@Override
	protected createNode(name, Map attributes) {
		if (unrecognizedElementDepth) {
			unrecognizedElementDepth++
			log.warn "ignoring node $name with attributes $attributes contained in unrecognized parent node"
			return
		}

		log.trace "createNode $name + attributes: $attributes"
	}

	@Override
	protected createNode(name, Map attributes, value) {
		if (unrecognizedElementDepth) {
			unrecognizedElementDepth++
			log.warn "ignoring node $name with value $value and attributes $attributes contained in unrecognized parent node"
			return
		}

		log.trace "createNode $name + value: $value attributes: $attributes"
	}

	@Override
	protected void setParent(parent, child) {
		log.trace "setParent $parent, child: $child"
		// do nothing
	}

	@Override
	protected void nodeCompleted(parent, node) {
		log.trace "nodeCompleted $parent $node"

		if (unrecognizedElementDepth) {
			unrecognizedElementDepth--
		}
		else {
			stack.pop()
		}
	}

	String toXml() {

		String env = Environment.current.name

		Map cacheEventListenerFactoriesXml = generateChildElementXmlMap(cacheEventListenerFactories,
				env, 'cacheEventListenerFactory')

		def factories = []
		if (bootstrapCacheLoaderFactory) {
			factories << bootstrapCacheLoaderFactory
		}
		Map bootstrapCacheLoaderFactoriesXml = generateChildElementXmlMap(factories,
				env, 'bootstrapCacheLoaderFactory')

		factories = []
		if (cacheExceptionHandlerFactory) {
			factories << cacheExceptionHandlerFactory
		}
		Map cacheExceptionHandlerFactoriesXml = generateChildElementXmlMap(factories,
						env, 'cacheExceptionHandlerFactory')

		Map cacheLoaderFactoriesXml = generateChildElementXmlMap(cacheLoaderFactories,
				env, 'cacheLoaderFactory')

		Map cacheExtensionFactoriesXml = generateChildElementXmlMap(cacheExtensionFactories,
				env, 'cacheExtensionFactory')

		StringBuilder xml = new StringBuilder()
		xml.append '<ehcache xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="ehcache.xsd"'

		String maxBytesLocalHeap = getValue(provider, 'maxBytesLocalHeap', '0')
		boolean maxBytesLocalHeapSet = maxBytesLocalHeap != '0'

		appendProperty xml, 'defaultTransactionTimeoutInSeconds', getValue(provider, 'defaultTransactionTimeoutInSeconds', 15), ' '
		appendProperty xml, 'dynamicConfig', getValue(provider, 'dynamicConfig', true), ' '
		appendProperty xml, 'monitoring', getValue(provider, 'monitoring', 'autodetect'), ' '
		appendProperty xml, 'updateCheck', getValue(provider, 'updateCheck', false), ' '
		appendNonZeroProperty xml, 'maxBytesLocalDisk', getValue(provider, 'maxBytesLocalDisk', 0), ' '
//		appendNonZeroProperty xml, 'maxBytesLocalHeap', maxBytesLocalHeap, ' '
		appendNonZeroProperty xml, 'maxBytesLocalOffHeap', getValue(provider, 'maxBytesLocalOffHeap', 0), ' '
		appendNonZeroProperty xml, 'maxEntriesLocalHeap', getValue(provider, 'maxEntriesLocalHeap', 10000), ' '
		appendProperty xml, 'name', provider.name ?: 'grails-cache-ehcache', ' '

		xml.append('>').append LF

		if (diskStore) {
			xml.append """$LF$INDENT<diskStore path="$diskStore" />$LF"""
		}

        if (sizeOfPolicy) {
            xml.append """$LF$INDENT<sizeOfPolicy"""
            appendProperty xml, 'maxDepth', getValue(sizeOfPolicy, 'maxDepth', 100), ' '
            appendProperty xml, 'maxDepthExceededBehavior', getValue(sizeOfPolicy, 'maxDepthExceededBehavior', 'abort'), ' '
            xml.append """/>$LF"""
        }

		if (defaultCache == null) {
			defaultCache = DEFAULT_DEFAULT_CACHE
		}

		appendCache xml, 'defaultCache', defaultCache, env, cacheEventListenerFactoriesXml,
			bootstrapCacheLoaderFactoriesXml, cacheExceptionHandlerFactoriesXml,
			cacheLoaderFactoriesXml, cacheExtensionFactoriesXml, maxBytesLocalHeapSet

		for (data in cacheManagerPeerProviderFactories) {
			appendCacheManagerPeerProviderFactory xml, data, env
		}

		appendCacheManagerPeerListenerFactory xml, env

		appendCacheManagerEventListenerFactory xml, env

		for (data in caches) {
			appendCache xml, 'cache', data, env, cacheEventListenerFactoriesXml,
				bootstrapCacheLoaderFactoriesXml, cacheExceptionHandlerFactoriesXml,
				cacheLoaderFactoriesXml, cacheExtensionFactoriesXml, maxBytesLocalHeapSet
		}

		xml.append(LF).append('</ehcache>').append LF

		xml.toString()
	}

	protected void appendCache(StringBuilder xml, String type, Map data, String env,
			Map cacheEventListenerFactoriesXml, Map bootstrapCacheLoaderFactoriesXml,
			Map cacheExceptionHandlerFactoriesXml, Map cacheLoaderFactoriesXml,
			Map cacheExtensionFactoriesXml, boolean maxBytesLocalHeapSet) {

		if (data.domain) {
			// collection
			data.name = "${data.domain}.$data.name"
		}

		if (!isValidInEnv(data, env)) {
			log.debug "skipping cache $data.name since it's not valid in env '$env'"
			return
		}

		xml.append "$LF$INDENT<$type "

		String name = data.name
		if (name) {
			xml.append('name="').append(name).append('"')
		}

		List cacheEventListenerFactoryNames = data.cacheEventListenerFactoryName
		List bootstrapCacheLoaderFactoryNames = data.bootstrapCacheLoaderFactoryName
		List cacheExceptionHandlerFactoryNames = data.cacheExceptionHandlerFactoryName
		List cacheLoaderFactoryNames = data.cacheLoaderFactoryName
		List cacheExtensionFactoryNames = data.cacheExtensionFactoryName

		if (!maxBytesLocalHeapSet && !data.maxBytesLocalHeap && !data.maxElementsInMemory) {
//			data.maxBytesLocalHeap = '10m'
		}

		data.each { key, value ->
			if (key in CACHE_NONPROPERTY_NAMES) return

			xml.append LF
			xml.append INDENT
			appendProperty xml, key, value, '       ', true
		}

		StringBuilder children = new StringBuilder()
		appendFactoryXmls children, cacheEventListenerFactoryNames, cacheEventListenerFactoriesXml
		appendFactoryXmls children, bootstrapCacheLoaderFactoryNames, bootstrapCacheLoaderFactoriesXml
		appendFactoryXmls children, cacheExceptionHandlerFactoryNames, cacheExceptionHandlerFactoriesXml
		appendFactoryXmls children, cacheLoaderFactoryNames, cacheLoaderFactoriesXml
		appendFactoryXmls children, cacheExtensionFactoryNames, cacheExtensionFactoriesXml

		if (children) {
			xml.append '>'
			xml.append LF
			xml.append children
			xml.append(INDENT).append '</cache>'
		}
		else {
			xml.append(LF).append(INDENT).append '/>'
		}

		xml.append LF
	}

	protected void appendCacheManagerPeerProviderFactory(StringBuilder xml, Map data, String env) {

		if (!isValidInEnv(data, env)) {
			log.debug "skipping cacheManagerPeerProviderFactory $data.className since it's not valid in env '$env'"
			return
		}

		String timeToLive = data.timeToLive
		if (timeToLive && TTL[timeToLive]) {
			data.timeToLive = TTL[timeToLive] // replace string with number
		}

		String className = data.className
		String type = data.factoryType
		switch (type) {
			case 'rmi':
				def rmiUrls = data.rmiUrls
				if (rmiUrls) {
					data.peerDiscovery = 'manual'
					data.rmiUrls = rmiUrls.join('|')
				}
				else {
					data.peerDiscovery = 'automatic'
				}

				appendCacheManagerPeerProviderFactoryNode xml, data, ',', className

				break

			case 'jgroups':
				appendCacheManagerPeerProviderFactoryNode xml, data, '::', className
				break

			case 'jms':
				appendCacheManagerPeerProviderFactoryNode xml, data, ',', className
				break

			default: throw new RuntimeException("Unknown cache manager type $type")
		}
	}

	protected void appendCacheManagerPeerProviderFactoryNode(StringBuilder xml, Map data,
			String delimiter, String className) {

		String properties = joinProperties(data, delimiter, ['className', 'factoryType'])
		appendSimpleNodeWithProperties xml, 'cacheManagerPeerProviderFactory', className, properties, delimiter
	}

	protected void appendCacheManagerPeerListenerFactory(StringBuilder xml, String env) {
		if (cacheManagerPeerListenerFactory == null) {
			return
		}

		if (!isValidInEnv(cacheManagerPeerListenerFactory, env)) {
			log.debug "skipping cacheManagerPeerListenerFactory since it's not valid in env '$env'"
			return
		}

		String className = cacheManagerPeerListenerFactory.className
		String properties = joinProperties(cacheManagerPeerListenerFactory, ',', ['className'])

		appendSimpleNodeWithProperties xml, 'cacheManagerPeerListenerFactory', className, properties, ','
	}

	protected void appendCacheManagerEventListenerFactory(StringBuilder xml, String env) {
		if (!cacheManagerEventListenerFactory) {
			return
		}

		if (!isValidInEnv(cacheManagerEventListenerFactory, env)) {
			log.debug "skipping cacheManagerEventListenerFactory since it's not valid in env '$env'"
			return
		}

		String className = cacheManagerEventListenerFactory.className
		String properties = joinProperties(cacheManagerEventListenerFactory, ',', ['className'])

		appendSimpleNodeWithProperties xml, 'cacheManagerEventListenerFactory', className, properties, ','
	}

	protected String generateChildElementXml(Map data, String nodeName) {

		def xml = new StringBuilder()

		String type = data.factoryType
		String className = data.className
		String properties = joinProperties(data, ',', ['factoryType', 'className', 'name'])

		appendSimpleNodeWithProperties xml, nodeName, className, properties, ',', 2

		xml.toString()
	}

	protected String joinProperties(Map data, String delimiter, List ignoredNames) {

		StringBuilder properties = new StringBuilder()
		String delim = ''
		data.each { key, value ->
			if (key == 'env' || key in ignoredNames) return
			appendProperty properties, key, value, delim, false
			delim = delimiter
		}

		properties.toString()
	}

	protected void appendNonZeroProperty(StringBuilder sb, String name, value, String prefix, boolean quote = true) {
		if ('0'.equals(value)) {
			return
		}

		sb.append(prefix).append(name).append('=')
		if (quote) sb.append('"')
		sb.append value
		if (quote) sb.append('"')
	}

	protected void appendProperty(StringBuilder sb, String name, value, String prefix, boolean quote = true) {
		sb.append(prefix).append(name).append('=')
		if (quote) sb.append('"')
		sb.append value
		if (quote) sb.append('"')
	}

	protected boolean isValidInEnv(Map data, String env) {
		def environments = data.env ?: []
		if (!(environments instanceof List)) {
			environments = [environments]
		}

		environments.isEmpty() || environments.contains(env)
	}

	protected void appendSimpleNodeWithProperties(StringBuilder xml, String nodeName, String className,
				String properties, String delimiter, int indentCount = 1) {

		String indent = INDENT.multiply(indentCount)
		xml.append "$LF$indent<$nodeName class='$className'"
		if (properties) {
			xml.append """$LF$indent${INDENT}properties="$properties"$LF"""
			xml.append "$indent${INDENT}propertySeparator='$delimiter'$LF$indent"
		}
		else {
			xml.append ' '
		}
		xml.append "/>$LF"
	}

	protected void addToList(Map data, String listName, value) {
		List list = data[listName]
		if (!list) {
			list = []
			data[listName] = list
		}

		list << value
	}

	protected Map generateChildElementXmlMap(List maps, String env, String nodeName) {
		Map xmls = [:]
		for (data in maps) {
			if (isValidInEnv(data, env)) {
				String name = data.name
				xmls[name] = generateChildElementXml(data, nodeName)
			}
		}
		xmls
	}

	protected void appendFactoryXmls(StringBuilder xml, List names, Map xmls) {
		for (name in names) {
			String factoryXml = xmls[name]
			if (factoryXml) {
				xml.append factoryXml
			}
		}
	}

	protected String getValue(Map m, String name, defaultIfNotSpecified) {
		def value = m[name]
		if (value == null) {
			value = defaultIfNotSpecified
		}
		value
	}

	protected void resolveProperties() {
		mergeCaches()

		setDefaults()

		resolveCacheManagerPeerListenerFactoryProperties()
		resolveCacheManagerPeerProviderFactoryProperties()
		resolveBootstrapCacheLoaderFactoryProperties()
		resolveCacheEventListenerFactoryProperties()

		mergeFactories cacheLoaderFactories
		mergeFactories cacheExtensionFactories
	}

	protected void setDefaults() {
		for (data in caches) {
			Map<String, Object> withDefaults = [:]
			withDefaults.putAll defaults
			withDefaults.putAll data
			data.clear()
			data.putAll withDefaults
		}
	}

	protected void mergeCaches() {
		mergeDefinitions caches, 'name'
	}

	protected void mergeFactories(List<Map<String, Object>> factories) {
		mergeDefinitions factories, 'className'
	}

	protected void mergeDefinitions(List<Map<String, Object>> definitions, String propertyName) {
		int count = definitions.size()
		for (int i = 0; i < count; i++) {
			for (int j = i + 1; j < count; j++) {
				if (definitions[j][propertyName] == definitions[i][propertyName]) {
					definitions[i].putAll definitions[j]
					definitions.remove j
					count--
					j--
				}
			}
		}
	}

	protected void resolveCacheManagerPeerListenerFactoryProperties() {
		if (cacheManagerPeerListenerFactory && !cacheManagerPeerListenerFactory.className) {
			cacheManagerPeerListenerFactory.className = 'net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory'
		}
	}

	protected void resolveCacheManagerPeerProviderFactoryProperties() {
		for (data in cacheManagerPeerProviderFactories) {
			String type = data.factoryType
			if (type && !data.className) {
				data.className = CACHE_MANAGER_PEER_PROVIDERS[type]
			}
		}
		mergeFactories cacheManagerPeerProviderFactories
	}

	protected void resolveBootstrapCacheLoaderFactoryProperties() {
		if (bootstrapCacheLoaderFactory) {
			resolveClassName([bootstrapCacheLoaderFactory], BOOTSTRAP_CACHE_LOADER_FACTORIES)
		}
	}

	protected void resolveCacheEventListenerFactoryProperties() {
		resolveClassName cacheEventListenerFactories, CACHE_EVENT_LISTENER_FACTORIES
		mergeFactories cacheEventListenerFactories
	}

	protected void resolveClassName(List<Map<String, Object>> definitions, Map<String, String> classNames) {
		for (data in definitions) {
			String type = data.factoryType
			if (type && !data.className) {
				data.className = classNames[type]
			}
		}
	}
}
