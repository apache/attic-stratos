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

package org.apache.stratos.gce.extension.config;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to read and store system properties
 */
public class GCEContext {
    private static final Log log = LogFactory.getLog(GCEContext.class);

    private static volatile GCEContext context;

    //cep stat publisher properties
    private boolean cepStatsPublisherEnabled;
    private String thriftReceiverIp;
    private String thriftReceiverPort;

    //IaaS properties
    private String projectName;
    private String projectID;
    private String regionName;
    private String keyFilePath;
    private String gceAccountID;
    private String networkName;

    //healthcheck properties
    private String healthCheckRequestPath;
    private String healthCheckPort;
    private String healthCheckTimeOutSec;
    private String healthCheckUnhealthyThreshold;

    //other properties
    private String namePrefix;
    private String operationTimeout;
    private String log4jPropertiesFileName;

    private GCEContext() {

       /* this.cepStatsPublisherEnabled = Boolean.getBoolean(Constants.CEP_STATS_PUBLISHER_ENABLED);
        this.thriftReceiverIp = System.getProperty(Constants.THRIFT_RECEIVER_IP);
        this.thriftReceiverPort = System.getProperty(Constants.THRIFT_RECEIVER_PORT);
        this.namePrefix = System.getProperty(Constants.NAME_PREFIX);
        this.projectName = System.getProperty(Constants.PROJECT_NAME);
        this.projectID = System.getProperty(Constants.PROJECT_ID);
        this.zoneName = System.getProperty(Constants.ZONE_NAME);
        this.regionName = System.getProperty(Constants.REGION_NAME);
        this.keyFilePath = System.getProperty(Constants.KEY_FILE_PATH);
        this.gceAccountID = System.getProperty(Constants.GCE_ACCOUNT_ID);
        this.healthCheckRequestPath = System.getProperty(Constants.HEALTH_CHECK_REQUEST_PATH);
        this.healthCheckPort = System.getProperty(Constants.HEALTH_CHECK_PORT);
        this.healthCheckTimeOutSec = System.getProperty(Constants.HEALTH_CHECK_TIME_OUT_SEC);
        this.healthCheckUnhealthyThreshold = System.getProperty(Constants.HEALTH_CHECK_UNHEALTHY_THRESHOLD);
        this.networkName = System.getProperty(Constants.NETWORK_NAME);
        this.operationTimeout = System.getProperty(Constants.OPERATION_TIMEOUT);


        if (log.isDebugEnabled()) {
            log.debug(Constants.CEP_STATS_PUBLISHER_ENABLED + " = " + cepStatsPublisherEnabled);
            log.debug(Constants.THRIFT_RECEIVER_IP + " = " + thriftReceiverIp);
            log.debug(Constants.THRIFT_RECEIVER_PORT + " = " + thriftReceiverPort);
            log.debug(Constants.THRIFT_RECEIVER_PORT + " = " + thriftReceiverPort);
            log.debug(Constants.NAME_PREFIX + " = " + namePrefix);
            log.debug(Constants.PROJECT_NAME + " = " + projectName);
            log.debug(Constants.PROJECT_ID + " = " + projectID);
            log.debug(Constants.ZONE_NAME + " = " + zoneName);
            log.debug(Constants.REGION_NAME + " = " + regionName);
            log.debug(Constants.KEY_FILE_PATH + " = " + keyFilePath);
            log.debug(Constants.GCE_ACCOUNT_ID + " = " + gceAccountID);
            log.debug(Constants.HEALTH_CHECK_REQUEST_PATH + " = " + healthCheckRequestPath);
            log.debug(Constants.HEALTH_CHECK_PORT + " = " + healthCheckPort);
            log.debug(Constants.HEALTH_CHECK_TIME_OUT_SEC + " = " + healthCheckTimeOutSec);
            log.debug(Constants.HEALTH_CHECK_UNHEALTHY_THRESHOLD + " = " + healthCheckUnhealthyThreshold);
            log.debug(Constants.NETWORK_NAME + "=" + networkName);
            log.debug(Constants.OPERATION_TIMEOUT + "=" + operationTimeout);

        }
        */

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

    public String getLog4jPropertiesFileName() {
        return log4jPropertiesFileName;
    }

    public void setLog4jPropertiesFileName(String log4jPropertiesFileName) {
        this.log4jPropertiesFileName = log4jPropertiesFileName;
    }

    public void validate() {

        validateSystemProperty(Constants.CEP_STATS_PUBLISHER_ENABLED);
        validateSystemProperty(Constants.NAME_PREFIX);
        validateSystemProperty(Constants.PROJECT_NAME);
        validateSystemProperty(Constants.PROJECT_ID);
        validateSystemProperty(Constants.REGION_NAME);
        validateSystemProperty(Constants.KEY_FILE_PATH);
        validateSystemProperty(Constants.GCE_ACCOUNT_ID);
        validateSystemProperty(Constants.HEALTH_CHECK_REQUEST_PATH);
        validateSystemProperty(Constants.HEALTH_CHECK_PORT);
        validateSystemProperty(Constants.HEALTH_CHECK_TIME_OUT_SEC);
        validateSystemProperty(Constants.HEALTH_CHECK_UNHEALTHY_THRESHOLD);
        validateSystemProperty(Constants.NETWORK_NAME);
        validateSystemProperty(Constants.OPERATION_TIMEOUT);

        if (cepStatsPublisherEnabled) {
            validateSystemProperty(Constants.THRIFT_RECEIVER_IP);
            validateSystemProperty(Constants.THRIFT_RECEIVER_PORT);
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

    public String getNamePrefix() {
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getKeyFilePath() {
        return keyFilePath;
    }

    public void setKeyFilePath(String keyFilePath) {
        this.keyFilePath = keyFilePath;
    }

    public String getGceAccountID() {
        return gceAccountID;
    }

    public void setGceAccountID(String gceAccountID) {
        this.gceAccountID = gceAccountID;
    }

    public String getHealthCheckRequestPath() {
        return healthCheckRequestPath;
    }

    public void setHealthCheckRequestPath(String healthCheckRequestPath) {
        this.healthCheckRequestPath = healthCheckRequestPath;
    }

    public String getHealthCheckPort() {
        return healthCheckPort;
    }

    public void setHealthCheckPort(String healthCheckPort) {
        this.healthCheckPort = healthCheckPort;
    }

    public String getHealthCheckTimeOutSec() {
        return healthCheckTimeOutSec;
    }

    public void setHealthCheckTimeOutSec(String healthCheckTimeOutSec) {
        this.healthCheckTimeOutSec = healthCheckTimeOutSec;
    }

    public String getHealthCheckUnhealthyThreshold() {
        return healthCheckUnhealthyThreshold;
    }

    public void setHealthCheckUnhealthyThreshold(String healthCheckUnhealthyThreshold) {
        this.healthCheckUnhealthyThreshold = healthCheckUnhealthyThreshold;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getOperationTimeout() {
        return operationTimeout;
    }

    public void setOperationTimeout(String operationTimeout) {
        this.operationTimeout = operationTimeout;
    }

    public void setCepStatsPublisherEnabled(boolean cepStatsPublisherEnabled) {
        this.cepStatsPublisherEnabled = cepStatsPublisherEnabled;
    }

    public void setThriftReceiverIp(String thriftReceiverIp) {
        this.thriftReceiverIp = thriftReceiverIp;
    }

    public void setThriftReceiverPort(String thriftReceiverPort) {
        this.thriftReceiverPort = thriftReceiverPort;
    }

}
