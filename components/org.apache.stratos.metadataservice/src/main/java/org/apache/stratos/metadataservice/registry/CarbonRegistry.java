/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.metadataservice.registry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.definition.MetaDataProperty;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.*;

/**
 * Carbon registry implementation
 */

public class CarbonRegistry extends AbstractAdmin implements DataStore {

	private static Log log = LogFactory.getLog(CarbonRegistry.class);
	@Context
	HttpServletRequest httpServletRequest;

	private static final String mainResource = "/stratos/";

	public CarbonRegistry() {

	}

	/**
	 * Get Properties of clustor
	 *
	 * @param applicationName ID of the application.
	 * @param clusterId Cluster ID
	 * @return
	 * @throws RegistryException
	 */
	public List<MetaDataProperty> getPropertiesOfCluster(String applicationName,
	                                                String clusterId) throws RegistryException {
		Registry tempRegistry = getGovernanceUserRegistry();
		String resourcePath = mainResource + applicationName + "/" + clusterId;
		if (!tempRegistry.resourceExists(resourcePath)) {
			return null;
			//throw new RegistryException("Cluster does not exist at " + resourcePath);
		}
		Resource regResource = tempRegistry.get(resourcePath);

		ArrayList<MetaDataProperty> newProperties = new ArrayList<MetaDataProperty>();

		Properties props = regResource.getProperties();
		Enumeration<?> x = props.propertyNames();
		while (x.hasMoreElements()) {
			String key = (String) x.nextElement();
			List<String> values = regResource.getPropertyValues(key);
			MetaDataProperty property = new MetaDataProperty();
			property.setKey(key);
			String[] valueArr = new String[values.size()];
			property.setValues(values.toArray(valueArr));

			newProperties.add(property);
		}

		return newProperties;
	}

	/**
	 * Add property to cluster
	 *
	 * @param applicationId ID of the application.
	 * @param clusterId Cluster ID
	 * @param property Propertys
	 * @throws RegistryException
	 */
	public void addPropertyToCluster(String applicationId, String clusterId, MetaDataProperty property)
			throws RegistryException {
		Registry tempRegistry = getGovernanceUserRegistry();
		String resourcePath = mainResource + applicationId + "/" + clusterId;
		Resource regResource = createOrGetResourceforCluster(tempRegistry, resourcePath);

		regResource.setProperty(property.getKey(), Arrays.asList(property.getValues()));
		tempRegistry.put(resourcePath, regResource);
		log.info(String.format("Property %s is added to cluster %s of application %s", property.getKey(), clusterId,
		                       applicationId));

	}

	/**
	 * Delete the resource identified by the applicationId, if exist.
	 *
	 * @param applicationId ID of the application.
	 * @return True if resource exist and able to delete, else false.
	 * @throws RegistryException
	 */
	public boolean deleteApplication(String applicationId) throws RegistryException {
		if (StringUtils.isEmpty(applicationId)) {
			throw new IllegalArgumentException("Application ID can not be null");
		}
		Registry tempRegistry = getGovernanceUserRegistry();
		String resourcePath = mainResource + applicationId;

		if (tempRegistry.resourceExists(resourcePath)) {
			tempRegistry.delete(resourcePath);
			log.info(String.format("Application removed from registry %s", applicationId));
			return true;
		}

		return false;
	}

	/**
	 * Add properties to cluster
	 *
	 * @param applicationName Name of the application
	 * @param clusterId Cluster id of the application
	 * @param properties Properties of the application
	 * @throws RegistryException
	 */
	public void addPropertiesToCluster(String applicationName, String clusterId, MetaDataProperty[] properties)
			throws RegistryException {
		Registry tempRegistry = getGovernanceUserRegistry();
		String resourcePath = mainResource + applicationName + "/" + clusterId;
		Resource regResource;
		regResource = createOrGetResourceforCluster(tempRegistry, resourcePath);

		for (MetaDataProperty property : properties) {
			regResource.setProperty(property.getKey(), (Arrays.asList(property.getValues())));

		}
		tempRegistry.put(resourcePath, regResource);
		log.info(String.format("Properties  are added to cluster %s of application %s", clusterId, applicationName));
	}

	/**
	 * Create or get resource for application
	 *
	 * @param tempRegistry Temp registry
	 * @param resourcePath Resource path
	 * @return Resource
	 * @throws RegistryException
	 */
	private Resource createOrGetResourceforApplication(Registry tempRegistry, String resourcePath)
			throws RegistryException {
		Resource regResource;
		if (tempRegistry.resourceExists(resourcePath)) {
			regResource = tempRegistry.get(resourcePath);
		} else {
			regResource = tempRegistry.newCollection();
			if (log.isDebugEnabled()) {
				log.debug("Registry resource is create at path " + regResource.getPath() + " for application");
			}
		}
		return regResource;
	}

	/**
	 * Create and get resources for Clustor
	 *
	 * @param tempRegistry Temp registry
	 * @param resourcePath Resource path
	 * @return
	 * @throws RegistryException
	 */
	private Resource createOrGetResourceforCluster(Registry tempRegistry, String resourcePath)
			throws RegistryException {

		int index = resourcePath.lastIndexOf('/');
		String applicationResourcePath = resourcePath.substring(0, index);
		createOrGetResourceforApplication(tempRegistry, applicationResourcePath);
		Resource regResource;
		if (tempRegistry.resourceExists(resourcePath)) {
			regResource = tempRegistry.get(resourcePath);
		} else {
			regResource = tempRegistry.newResource();
			if (log.isDebugEnabled()) {
				log.debug("Registry resource is create at path for cluster" + regResource.getPath() + " for cluster");
			}
		}
		return regResource;
	}

}
