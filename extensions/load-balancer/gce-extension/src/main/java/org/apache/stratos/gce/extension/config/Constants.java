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

/**
 * GCE extension constants.
 */
public class Constants {

    public static final String CONFIG_FILE_NAME = "gce-configuration.xml";
    public static final String CONFIG_FOLDER_NAME = "conf";

    //CEP configuration
    public static final String CEP_STATS_PUBLISHER_ELEMENT = "cepStatsPublisher";
    public static final String CEP_STATS_PUBLISHER_ENABLED = "enable";
    public static final String THRIFT_RECEIVER_IP = "thriftReceiverIp";
    public static final String THRIFT_RECEIVER_PORT = "thriftReceiverPort";

    //IaaS provider configuration
    public static final String IAAS_PROPERTIES_ELEMENT = "iaasProperties";
    public static final String PROJECT_NAME = "projectName";
    public static final String PROJECT_ID = "projectId";
    public static final String REGION_NAME = "regionName";
    public static final String KEY_FILE_PATH = "keyFilePath";
    public static final String GCE_ACCOUNT_ID = "gceAccountId";
    public static final String NETWORK_NAME = "networkName";

    //health check configuration
    public static final String HEALTH_CHECK_PROPERTIES_ELEMENT = "healthCheckProperties";
    public static final String HEALTH_CHECK_REQUEST_PATH = "healthCheckRequestPath";
    public static final String HEALTH_CHECK_PORT = "healthCheckPort";
    public static final String HEALTH_CHECK_INTERVAL_SEC= "healthCheckIntervalSec";
    public static final String HEALTH_CHECK_TIME_OUT_SEC = "healthCheckTimeoutSec";
    public static final String HEALTH_CHECK_UNHEALTHY_THRESHOLD = "healthCheckUnhealthyThreshold";

    //other properties
    public static final String OPERATION_TIMEOUT = "operationTimeout";
    public static final String NAME_PREFIX = "namePrefix";
    public static final String LOG4J_PROPERTIES_FILE_NAME = "log4jPropertiesFileName";
}
