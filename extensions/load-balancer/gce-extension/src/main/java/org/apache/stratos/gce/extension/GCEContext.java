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

package org.apache.stratos.gce.extension;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to read and store system properties
 */
public class GCEContext {
    private static final Log log = LogFactory.getLog(GCEContext.class);

    private static volatile GCEContext context;

    private boolean cepStatsPublisherEnabled;
    private String thriftReceiverIp;
    private String thriftReceiverPort;
    private String networkPartitionId;
    private String clusterId;
    private String serviceName;
    private String namePrefix;


    private GCEContext() {

        this.cepStatsPublisherEnabled = Boolean.getBoolean(Constants.CEP_STATS_PUBLISHER_ENABLED);
        this.thriftReceiverIp = System.getProperty(Constants.THRIFT_RECEIVER_IP);
        this.thriftReceiverPort = System.getProperty(Constants.THRIFT_RECEIVER_PORT);
        this.networkPartitionId = System.getProperty(Constants.NETWORK_PARTITION_ID);
        this.clusterId = System.getProperty(Constants.CLUSTER_ID);
        this.serviceName = System.getProperty(Constants.SERVICE_NAME);
        this.namePrefix = System.getProperty(Constants.NAME_PREFIX);

        if (log.isDebugEnabled()) {
            log.debug(Constants.CEP_STATS_PUBLISHER_ENABLED + " = " + cepStatsPublisherEnabled);
            log.debug(Constants.THRIFT_RECEIVER_IP + " = " + thriftReceiverIp);
            log.debug(Constants.THRIFT_RECEIVER_PORT + " = " + thriftReceiverPort);
            log.debug(Constants.THRIFT_RECEIVER_PORT + " = " + thriftReceiverPort);
            log.debug(Constants.NETWORK_PARTITION_ID + " = " + networkPartitionId);
            log.debug(Constants.CLUSTER_ID + " = " + clusterId);
            log.debug(Constants.NAME_PREFIX + " = "+ namePrefix);
        }

    }

    public static GCEContext getInstance() {
        if (context == null) {
            synchronized (GCEContext.class) {
                if (context == null) {
                    context = new GCEContext();
                }
            }
        }
        return context;
    }

    public void validate() {

        validateSystemProperty(Constants.CEP_STATS_PUBLISHER_ENABLED);
        validateSystemProperty(Constants.CLUSTER_ID);
        validateSystemProperty(Constants.NAME_PREFIX);

        if (cepStatsPublisherEnabled) {
            validateSystemProperty(Constants.THRIFT_RECEIVER_IP);
            validateSystemProperty(Constants.THRIFT_RECEIVER_PORT);
            validateSystemProperty(Constants.NETWORK_PARTITION_ID);

        }
    }

    private void validateSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        if (StringUtils.isEmpty(value)) {
            throw new RuntimeException("System property was not found: " + propertyName);
        }
    }

    public boolean isCEPStatsPublisherEnabled() {
        return cepStatsPublisherEnabled;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getNamePrefix() { return namePrefix; }


}
