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
package org.apache.stratos.messaging.util;

public class Constants {
	/**
	 * Enum for Messaging topics
	 */
	public enum Topics {
		TOPOLOGY_TOPIC("topology/#"),
		HEALTH_STAT_TOPIC("summarized-health-stats"),
		INSTANCE_STATUS_TOPIC("instance/status/#"),
		INSTANCE_NOTIFIER_TOPIC("instance/notifier/#"),
		APPLICATIONS_TOPIC("applications/#"),
		CLUSTER_STATUS_TOPIC("cluster/status/#"),
		TENANT_TOPIC("tenant/#"),
		PING_TOPIC("ping");


		private String topicName;

		private Topics(String topicName) {
			this.topicName = topicName;
		}

		/**
		 * Get the topic name
		 * @return topic name
		 */
		public String getTopicName() {
			return topicName;
		}

	}


	public static final String TENANT_RANGE_ALL = "*";
	public static final String TENANT_RANGE_DELIMITER = "-";


	/* Topology filter constants */
	public static final String FILTER_VALUE_ASSIGN_OPERATOR = "=";
	public static final String FILTER_KEY_VALUE_PAIR_SEPARATOR = "|";
	public static final String FILTER_VALUE_SEPARATOR = ",";

	public static final String TOPOLOGY_SERVICE_FILTER = "stratos.topology.service.filter";
	public static final String TOPOLOGY_SERVICE_FILTER_SERVICE_NAME = "service-name";

	public static final String TOPOLOGY_CLUSTER_FILTER = "stratos.topology.cluster.filter";
	public static final String TOPOLOGY_CLUSTER_FILTER_CLUSTER_ID = "cluster-id";

	public static final String TOPOLOGY_MEMBER_FILTER = "stratos.topology.member.filter";
	public static final String TOPOLOGY_MEMBER_FILTER_LB_CLUSTER_ID = "lb-cluster-id";


	// to identify a lb cluster
	public static final String IS_LOAD_BALANCER = "load.balancer";
	public static final String LOAD_BALANCER_REF = "load.balancer.ref";
	public static final String SERVICE_AWARE_LOAD_BALANCER = "service.aware.load.balancer";
	public static final String DEFAULT_LOAD_BALANCER = "default.load.balancer";
	public static final String NO_LOAD_BALANCER = "no.load.balancer";
	public static final String EXISTING_LOAD_BALANCERS = "existing.load.balancers";
	public static final String LOAD_BALANCED_SERVICE_TYPE = "load.balanced.service.type";


	public static final String IS_PRIMARY = "PRIMARY";

	// System Properties
	public static final String AVERAGE_PING_INTERVAL_PROPERTY =
	                                                            "stratos.messaging.averagePingInterval";
	public static final String FAILOVER_PING_INTERVAL_PROPERTY =
	                                                             "stratos.messaging.failoverPingInterval";

	// Default values
	public static final int DEFAULT_AVERAGE_PING_INTERVAL = 1000;
	public static final int DEFAULT_FAILOVER_PING_INTERVAL = 30000;

}
