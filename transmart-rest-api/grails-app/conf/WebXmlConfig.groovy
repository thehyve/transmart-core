/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Application configuration file for WebXml plugin.
 */
webxml {
	//========================================
	// Session Timeout
	//========================================
	//
	// uncomment to set session timeout - Be sure to specify value as an Integer
	// sessionConfig.sessionTimeout = 30

	//========================================
	// Delegating Filter Chain
	//========================================
	//
	// Add a 'filter chain proxy' delegater as a Filter.  This will allow the application
	// to define a FilterChainProxy bean that can add additional filters, such as
	// an instance of org.springframework.security.web.FilterChainProxy.

	// Set to true to add a filter chain delegator.
	//filterChainProxyDelegator.add = true

	// The name of the delegate FilterChainProxy bean.  You must ensure you have added a bean
	// with this name that implements FilterChainProxy to
	// YOUR-APP/grails-app/conf/spring/resources.groovy.
	//filterChainProxyDelegator.targetBeanName = "filterChainProxyDelegate"

	// The URL pattern to which the filter will apply.  Usually set to '/*' to cover all URLs.
	//filterChainProxyDelegator.urlPattern = "/*"

	// Set to true to add Listeners
	//listener.add = true
	//listener.classNames = ["org.springframework.web.context.request.RequestContextListener"]

	//-------------------------------------------------
	// These settings usually do not need to be changed
	//-------------------------------------------------

	// The name of the delegating filter.
	//filterChainProxyDelegator.filterName = "filterChainProxyDelegator"

	// The delegating filter proxy class.
	//filterChainProxyDelegator.className = "org.springframework.web.filter.DelegatingFilterProxy"

	// ------------------------------------------------
	// Example for context aparameters
	// ------------------------------------------------
	// this example will create the following XML part
	// contextparams = [port: '6001']
	//
	//  <context-param>
	//	<param-name>port</param-name>
	//	<param-value>6001</param-value>
	//  </context-param>
}
