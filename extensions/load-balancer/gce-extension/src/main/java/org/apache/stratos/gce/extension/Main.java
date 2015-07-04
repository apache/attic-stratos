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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.common.util.AxiomXpathParserUtil;
import org.apache.stratos.gce.extension.config.Constants;
import org.apache.stratos.gce.extension.config.GCEContext;
import org.apache.stratos.gce.extension.config.parser.GCEConfigParser;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;
import org.apache.stratos.load.balancer.extension.api.LoadBalancerExtension;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * GCE extension main class.
 */


public class Main {
    private static final Log log = LogFactory.getLog(Main.class);
    private static ExecutorService executorService;



    public static void main(String[] args) {

        LoadBalancerExtension extension = null;
        try {

            //read configuration from gce-configuration.xml and store configuration in GCEConfigurationHolder class
            File configFile = new File(getFilePathOfConfigFile(Constants.CONFIG_FILE_NAME));
            OMElement documentElement  = AxiomXpathParserUtil.parse(configFile);
            GCEConfigParser.parse(documentElement);

            // Configure log4j properties
            PropertyConfigurator.configure(getFilePathOfConfigFile(GCEContext.getInstance().getLog4jPropertiesFileName()));

            if (log.isInfoEnabled()) {
                log.info("GCE extension started");
            }

            // Add shutdown hook
            final Thread mainThread = Thread.currentThread();
            final LoadBalancerExtension finalExtension = extension;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        if (finalExtension != null) {
                            log.info("GCE gce instance...");
                            finalExtension.stop();
                        }
                        mainThread.join();
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            });

            executorService = StratosThreadPool.getExecutorService("gce.extension.thread.pool", 10);

            // Validate runtime parameters
            TopologyProvider topologyProvider = new TopologyProvider();

            //If user has enabled the cep stats publisher, create a stat publisher object. Else null
            GCEStatisticsReader statisticsReader = GCEContext.getInstance().isCEPStatsPublisherEnabled() ?
                    new GCEStatisticsReader(topologyProvider) : null;
            extension = new LoadBalancerExtension(new GCELoadBalancer(), statisticsReader, topologyProvider);
            extension.setExecutorService(executorService);
            extension.execute();

        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
            if (extension != null) {
                log.info("Shutting GCE instance...");
                extension.stop();
            }
        }
    }

    private static String getFilePathOfConfigFile(String fileName) {
        String workingDirectory = System.getProperty("user.dir");
        String FilePath = workingDirectory + File.separator + ".." + File.separator + Constants.CONFIG_FOLDER_NAME + File.separator + fileName;
        return FilePath;
    }
}