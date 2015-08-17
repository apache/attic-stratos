/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.iaases.ec2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.cloud.controller.services.impl.CloudControllerServiceUtil;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.Scope;

import java.util.Properties;


/**
 * AWS-EC2 {@link org.apache.stratos.cloud.controller.iaases.PartitionValidator} implementation.
 */
public class EC2PartitionValidator implements PartitionValidator {

    private static final Log log = LogFactory.getLog(EC2PartitionValidator.class);
    private IaasProvider iaasProvider;
    private Iaas iaas;

    @Override
    public IaasProvider validate(Partition partition, Properties properties) throws InvalidPartitionException {
        // validate the existence of the region and zone properties.
        try {
            if (properties.containsKey(Scope.REGION.toString())) {
                String region = properties.getProperty(Scope.REGION.toString());

                if (iaasProvider.getImage() != null && !iaasProvider.getImage().contains(region)) {

                    String message = "Invalid partition detected, invalid region. [partition-id] " + partition.getId() +
                            ", [region] " + region;
                    log.error(message);
                    throw new InvalidPartitionException(message);
                }

                iaas.isValidRegion(region);

                IaasProvider updatedIaasProvider = new IaasProvider(iaasProvider);
                Iaas updatedIaas = CloudControllerServiceUtil.buildIaas(updatedIaasProvider);
                updatedIaas.setIaasProvider(updatedIaasProvider);

                if (properties.containsKey(Scope.ZONE.toString())) {
                    String zone = properties.getProperty(Scope.ZONE.toString());
                    iaas.isValidZone(region, zone);
                    updatedIaasProvider.setProperty(CloudControllerConstants.AVAILABILITY_ZONE, zone);
                    updatedIaas = CloudControllerServiceUtil.buildIaas(updatedIaasProvider);
                    updatedIaas.setIaasProvider(updatedIaasProvider);
                }

                updateOtherProperties(updatedIaasProvider, properties);
                return updatedIaasProvider;

            } else {

                return iaasProvider;
            }
        } catch (Exception ex) {
            String message = "Invalid partition detected: [partition-id] " + partition.getId();
            throw new InvalidPartitionException(message, ex);
        }
    }

    private void updateOtherProperties(IaasProvider updatedIaasProvider,
                                       Properties properties) {
        Iaas updatedIaas;
        try {
            updatedIaas = CloudControllerServiceUtil.buildIaas(updatedIaasProvider);

            for (Object property : properties.keySet()) {
                if (property instanceof String) {
                    String key = (String) property;
                    updatedIaasProvider.setProperty(key,
                            properties.getProperty(key));
                    if (log.isDebugEnabled()) {
                        log.debug("Added property " + key
                                + " to the IaasProvider.");
                    }
                }
            }
            updatedIaas = CloudControllerServiceUtil.buildIaas(updatedIaasProvider);
            updatedIaas.setIaasProvider(updatedIaasProvider);
        } catch (InvalidIaasProviderException ignore) {
        }

    }

    @Override
    public void setIaasProvider(IaasProvider iaas) {
        this.iaasProvider = iaas;
        this.iaas = iaas.getIaas();
    }

}
