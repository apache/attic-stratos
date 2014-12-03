/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.haproxy.extension;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.common.util.ConfUtil;
import org.apache.stratos.load.balancer.extension.api.LoadBalancerExtension;

import java.util.concurrent.ExecutorService;

/**
 * HAProxy extension main class.
 */
public class Main {
	private static final Log log = LogFactory.getLog(Main.class);
	private static ExecutorService executorService;
	private static final String THREAD_IDENTIFIER_KEY = "threadPool.haproxyExtension.identifier";
	private static final String DEFAULT_IDENTIFIER = "haproxy-entension";
	private static final String THREAD_POOL_SIZE_KEY = "threadPool.haproxyExtension.threadPoolSize";
	private static final String COMPONENTS_CONFIG = "stratos-config";
	private static final int THREAD_POOL_SIZE = 10;

	public static void main(String[] args) {

		LoadBalancerExtension extension = null;
		try {
			// Configure log4j properties
			PropertyConfigurator.configure(System.getProperty("log4j.properties.file.path"));

			if (log.isInfoEnabled()) {
				log.info("HAProxy extension started");
			}
			XMLConfiguration conf = ConfUtil.getInstance(COMPONENTS_CONFIG).getConfiguration();
			int threadPoolSize = conf.getInt(THREAD_POOL_SIZE_KEY, THREAD_POOL_SIZE);
			String threadIdentifier = conf.getString(THREAD_IDENTIFIER_KEY, DEFAULT_IDENTIFIER);
			ExecutorService executorService = StratosThreadPool.getExecutorService(threadIdentifier, threadPoolSize);
			// Validate runtime parameters
			HAProxyContext.getInstance().validate();
			extension = new LoadBalancerExtension(new HAProxy(),
			                                      (HAProxyContext.getInstance().isCEPStatsPublisherEnabled() ?
			                                       new HAProxyStatisticsReader() : null));
			extension.setExecutorService(executorService);
			extension.execute();
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error(e);
			}
			if (extension != null) {
				extension.terminate();
			}
		}
	}
}
