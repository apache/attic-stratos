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

    private GCEContext(){

        this.cepStatsPublisherEnabled = Boolean.getBoolean(Constants.CEP_STATS_PUBLISHER_ENABLED);
        this.thriftReceiverIp = System.getProperty(Constants.THRIFT_RECEIVER_IP);
        this.thriftReceiverPort = System.getProperty(Constants.THRIFT_RECEIVER_PORT);
        if (log.isDebugEnabled()) {
            log.debug(Constants.CEP_STATS_PUBLISHER_ENABLED + " = " + cepStatsPublisherEnabled);
            log.debug(Constants.THRIFT_RECEIVER_IP + " = " + thriftReceiverIp);
            log.debug(Constants.THRIFT_RECEIVER_PORT + " = " + thriftReceiverPort);
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



}
