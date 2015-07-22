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
 * This class is used to store configuration properties for gce-extension
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
    private String healthCheckIntervalSec;
    private String healthCheckUnhealthyThreshold;
    private String healthCheckHealthyThreshold;

    //other properties
    private String namePrefix;
    private String operationTimeout;
    private String log4jPropertiesFileName;

    //private constructor
    private GCEContext() {
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
        validateProperty(Boolean.toString(cepStatsPublisherEnabled));
        validateProperty(namePrefix);
        validateProperty(projectName);
        validateProperty(projectID);
        validateProperty(regionName);
        validateProperty(keyFilePath);
        validateProperty(gceAccountID);
        validateProperty(healthCheckRequestPath);
        validateProperty(healthCheckPort);
        validateProperty(healthCheckTimeOutSec);
        validateProperty(healthCheckIntervalSec);
        validateProperty(healthCheckHealthyThreshold);
        validateProperty(healthCheckHealthyThreshold);
        validateProperty(networkName);
        validateProperty(operationTimeout);

        if (cepStatsPublisherEnabled) {
            validateProperty(Constants.THRIFT_RECEIVER_IP);
            validateProperty(Constants.THRIFT_RECEIVER_PORT);
        }
    }

    private void validateProperty(String propertyName) {
        if (StringUtils.isEmpty(propertyName)) {
            throw new RuntimeException("Property was not found: " + propertyName);
        }
    }

    public String getHealthCheckIntervalSec() {
        return healthCheckIntervalSec;
    }

    public void setHealthCheckIntervalSec(String healthCheckIntervalSec) {
        this.healthCheckIntervalSec = healthCheckIntervalSec;
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

    public String getHealthCheckHealthyThreshold() {
        return healthCheckHealthyThreshold;
    }

    public void setHealthCheckHealthyThreshold(String healthCheckHealthyThreshold) {
        this.healthCheckHealthyThreshold = healthCheckHealthyThreshold;
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
