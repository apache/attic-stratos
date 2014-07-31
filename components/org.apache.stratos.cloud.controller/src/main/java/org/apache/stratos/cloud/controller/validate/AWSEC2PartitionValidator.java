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
package org.apache.stratos.cloud.controller.validate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.apache.stratos.messaging.domain.topology.Scope;

import java.util.Properties;


/**
 * AWS-EC2 {@link PartitionValidator} implementation.
 * @author nirmal
 *
 */
public class AWSEC2PartitionValidator implements PartitionValidator {
    
    private static final Log log = LogFactory.getLog(AWSEC2PartitionValidator.class);
    private IaasProvider iaasProvider;
    private Iaas iaas;

    @Override
    public IaasProvider validate(String partitionId, Properties properties) throws InvalidPartitionException {
        // validate the existence of the region and zone properties.
        try {
            if (properties.containsKey(Scope.region.toString())) {
                String region = properties.getProperty(Scope.region.toString());
                
                if (iaasProvider.getImage() != null && !iaasProvider.getImage().contains(region)) {

                    String msg =
                                 "Invalid Partition Detected : " + partitionId +
                                         " - Cause: Invalid Region: " + region;
                    log.error(msg);
                    throw new InvalidPartitionException(msg);
                } 
                
                iaas.isValidRegion(region);
                
                IaasProvider updatedIaasProvider = new IaasProvider(iaasProvider);
                
                Iaas updatedIaas = CloudControllerUtil.getIaas(updatedIaasProvider);
                updatedIaas.setIaasProvider(updatedIaasProvider);
                
                if (properties.containsKey(Scope.zone.toString())) {
                    String zone = properties.getProperty(Scope.zone.toString());
                    iaas.isValidZone(region, zone);
                    updatedIaasProvider.setProperty(CloudControllerConstants.AVAILABILITY_ZONE, zone);
                    updatedIaas = CloudControllerUtil.getIaas(updatedIaasProvider);
                    updatedIaas.setIaasProvider(updatedIaasProvider);
                } 
                
                updateOtherProperties(updatedIaasProvider, properties);
                
                return updatedIaasProvider;
                
            } else {
                
                return iaasProvider;
            }
        } catch (Exception ex) {
            String msg = "Invalid Partition Detected : "+partitionId+". Cause: "+ex.getMessage();
            log.error(msg, ex);
            throw new InvalidPartitionException(msg, ex);
        }
        
    }

    private void updateOtherProperties(IaasProvider updatedIaasProvider,
			Properties properties) {
    	Iaas updatedIaas;
		try {
			updatedIaas = CloudControllerUtil.getIaas(updatedIaasProvider);

			for (Object property : properties.keySet()) {
				if (property instanceof String) {
					String key = (String) property;
					if (!Scope.zone.toString().equals(key)) {
						updatedIaasProvider.setProperty(key,
								properties.getProperty(key));
						if (log.isDebugEnabled()) {
							log.debug("Added property "+key+ " to the IaasProvider.");
						}
					}
				}
			}
			updatedIaas = CloudControllerUtil.getIaas(updatedIaasProvider);
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
