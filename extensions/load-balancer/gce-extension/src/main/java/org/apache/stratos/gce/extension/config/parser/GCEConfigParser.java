package org.apache.stratos.gce.extension.config.parser;

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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.exception.MalformedConfigurationFileException;
import org.apache.stratos.common.util.AxiomXpathParserUtil;
import org.apache.stratos.gce.extension.config.Constants;
import org.apache.stratos.gce.extension.config.GCEContext;

import javax.xml.namespace.QName;

public class GCEConfigParser {
    private static final Log log = LogFactory.getLog(GCEConfigParser.class);
    private static GCEContext gceContext;

    /**
     * Parse the gce-configuration.xml file when the extension is starting up.
     *
     * @param documentElement axiom document element.
     * @throws MalformedConfigurationFileException
     */
    public static void parse(OMElement documentElement) throws MalformedConfigurationFileException {

        //get cep info
        OMElement cepInfoElement = AxiomXpathParserUtil.getFirstChildElement(documentElement,
                Constants.CEP_STATS_PUBLISHER_ELEMENT);
        extractCepConfiguration(cepInfoElement);

        //get GCE IaaS info
        OMElement gceIaasInfoElement = AxiomXpathParserUtil.getFirstChildElement(documentElement,
                Constants.IAAS_PROPERTIES_ELEMENT);
        extractGceIaasInformation(gceIaasInfoElement);

        //get heath check info
        OMElement healthCheckPropertiesElement = AxiomXpathParserUtil.getFirstChildElement(documentElement,
                Constants.HEALTH_CHECK_PROPERTIES_ELEMENT);
        extractHealthCheckProperties(healthCheckPropertiesElement);

        //extract other properties
        extractOtherProperties(documentElement);

        //validate extracted properties
        gceContext.validate();



    }

    /**
     * Extract cep ip and port and store in gceContext object
     *
     * @param cepInfoElement
     */
    private static void extractCepConfiguration(OMElement cepInfoElement) {

        //Check whether the cep stat publisher enabled or not
        QName qName = new QName(Constants.CEP_STATS_PUBLISHER_ENABLED);
        String enabled = cepInfoElement.getAttributeValue(qName);

        gceContext = GCEContext.getInstance();
        gceContext.setCepStatsPublisherEnabled(Boolean.parseBoolean(enabled));

        //if stat publisher is enabled
        if (Boolean.parseBoolean(enabled)) {
            //extract rest configuration of cep

            String thriftReceiverIp = AxiomXpathParserUtil.getFirstChildElement(cepInfoElement, Constants.THRIFT_RECEIVER_IP).getText();
            String thriftReceiverPort = AxiomXpathParserUtil.getFirstChildElement(cepInfoElement, Constants.THRIFT_RECEIVER_PORT).getText();

            //set extracted ip and port to gceContext object
            gceContext.setThriftReceiverIp(thriftReceiverIp);
            gceContext.setThriftReceiverPort(thriftReceiverPort);

            //validate above properties

        }

    }

    /**
     * extract Iaas propreties from given OMElement and store it in gceContext object
     *
     * @param gceIaasInfoElement - OMElement which contains Iaas properties
     */
    private static void extractGceIaasInformation(OMElement gceIaasInfoElement) {

        String projectName = AxiomXpathParserUtil.getFirstChildElement(gceIaasInfoElement, Constants.PROJECT_NAME).getText();
        String projectId = AxiomXpathParserUtil.getFirstChildElement(gceIaasInfoElement, Constants.PROJECT_ID).getText();
        String regionName = AxiomXpathParserUtil.getFirstChildElement(gceIaasInfoElement, Constants.REGION_NAME).getText();
        String keyFilePath = AxiomXpathParserUtil.getFirstChildElement(gceIaasInfoElement, Constants.KEY_FILE_PATH).getText();
        String gceAccountId = AxiomXpathParserUtil.getFirstChildElement(gceIaasInfoElement, Constants.GCE_ACCOUNT_ID).getText();
        String networkName = AxiomXpathParserUtil.getFirstChildElement(gceIaasInfoElement, Constants.NETWORK_NAME).getText();

        //set extracted properties to gceContext object
        gceContext.setProjectName(projectName);
        gceContext.setProjectID(projectId);
        gceContext.setRegionName(regionName);
        gceContext.setKeyFilePath(keyFilePath);
        gceContext.setGceAccountID(gceAccountId);
        gceContext.setNetworkName(networkName);

    }

    /**
     * extract health check properties from given OMElement and store it in gceContext object
     * @param healthCheckPropertiesElement - OMElement which contains health check properties
     */
    private static void extractHealthCheckProperties(OMElement healthCheckPropertiesElement) {

        String healthCheckRequestPath = AxiomXpathParserUtil.getFirstChildElement(healthCheckPropertiesElement, Constants.HEALTH_CHECK_REQUEST_PATH).getText();
        String healthCheckPort = AxiomXpathParserUtil.getFirstChildElement(healthCheckPropertiesElement, Constants.HEALTH_CHECK_PORT).getText();
        String healthCheckTimeoutSec = AxiomXpathParserUtil.getFirstChildElement(healthCheckPropertiesElement, Constants.HEALTH_CHECK_TIME_OUT_SEC).getText();
        String healthCheckUnhealthyThreshold = AxiomXpathParserUtil.getFirstChildElement(healthCheckPropertiesElement, Constants.HEALTH_CHECK_UNHEALTHY_THRESHOLD).getText();
        String heathCheckIntervalSec = AxiomXpathParserUtil.getFirstChildElement(healthCheckPropertiesElement, Constants.HEALTH_CHECK_INTERVAL_SEC).getText();
        String healthCheckHealthyThreshold = AxiomXpathParserUtil.getFirstChildElement(healthCheckPropertiesElement, Constants.HEALTH_CHECK_HEALTHY_THRESHOLD).getText();


        //set extracted properties to gceContext object
        gceContext.setHealthCheckRequestPath(healthCheckRequestPath);
        gceContext.setHealthCheckPort(healthCheckPort);
        gceContext.setHealthCheckTimeOutSec(healthCheckTimeoutSec);
        gceContext.setHealthCheckUnhealthyThreshold(healthCheckUnhealthyThreshold);
        gceContext.setHealthCheckIntervalSec(heathCheckIntervalSec);
        gceContext.setHealthCheckHealthyThreshold(healthCheckHealthyThreshold);

    }

    /**
     * extract all other properties which is not extracted from above methods and store in gceContext object
     *
     * @param documentElement - OMElement which contains other properties
     */
    private static void extractOtherProperties(OMElement documentElement) {
        String operationTimeout = AxiomXpathParserUtil.getFirstChildElement(documentElement, Constants.OPERATION_TIMEOUT).getText();
        String namePrefix = AxiomXpathParserUtil.getFirstChildElement(documentElement, Constants.NAME_PREFIX).getText();
        String log4jPropertiesFileName = AxiomXpathParserUtil.getFirstChildElement(documentElement, Constants.LOG4J_PROPERTIES_FILE_NAME).getText();

        //set extracted properties to gceContext object
        gceContext.setOperationTimeout(operationTimeout);
        gceContext.setNamePrefix(namePrefix);
        gceContext.setLog4jPropertiesFileName(log4jPropertiesFileName);
    }

}
