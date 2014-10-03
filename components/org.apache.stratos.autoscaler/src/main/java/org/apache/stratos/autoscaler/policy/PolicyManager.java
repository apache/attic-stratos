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

package org.apache.stratos.autoscaler.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.stratos.common.constants.StratosConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.wso2.carbon.context.CarbonContext;

/**
 * Manager class for the purpose of managing Autoscale/Deployment policy definitions.
 */
public class PolicyManager {

    private static final Log log = LogFactory.getLog(PolicyManager.class);

    private static Map<Integer, Map<String, AutoscalePolicy>> autoscalePolicyListMap = new HashMap<Integer, Map<String, AutoscalePolicy>>();

    private static Map<Integer, Map<String, DeploymentPolicy>> deploymentPolicyListMap = new HashMap<Integer, Map<String, DeploymentPolicy>>();
    
    
    /* An instance of a PolicyManager is created when the class is loaded. 
     * Since the class is loaded only once, it is guaranteed that an object of 
     * PolicyManager is created only once. Hence it is singleton.
     */
    
    private static class InstanceHolder {
        private static final PolicyManager INSTANCE = new PolicyManager(); 
    }

    public static PolicyManager getInstance() {
        return InstanceHolder.INSTANCE;
     }
    
    private PolicyManager() {
    }
    
    /**
     * Checks and returns whether policies for tenant exists in information model
     *
     *@param tenantId
     *@return
     */
    public boolean isTenantPolicyDetailsInInformationModel(int tenantId) {
    	return (autoscalePolicyListMap.containsKey(tenantId) && deploymentPolicyListMap.containsKey(tenantId));
    }
    
    /**
     * Loads Autoscaling policies to information model
     *
     */
    public void loadASPoliciesToInformationModel() {
    	List<AutoscalePolicy> asPolicies = RegistryManager.getInstance(CarbonContext.getThreadLocalCarbonContext().getTenantId()).retrieveASPolicies();
        Iterator<AutoscalePolicy> asPolicyIterator = asPolicies.iterator();
        while (asPolicyIterator.hasNext()) {
            AutoscalePolicy asPolicy = asPolicyIterator.next();
	    	try {
	                addASPolicyToInformationModel(asPolicy);
	        }
	    	catch (InvalidPolicyException e) {
	    		log.warn("Unable to load Autoscalar policy to information Model ["+ asPolicy.getTenantId() + "]");
	    	}
    	}
    }
    
    /**
     * Loads Deployment policies to information model
     *
     */
    public void loadDeploymentPoliciesToInformationModel() {
    	List<DeploymentPolicy> depPolicies = RegistryManager.getInstance(CarbonContext.getThreadLocalCarbonContext().getTenantId()).retrieveDeploymentPolicies();
        Iterator<DeploymentPolicy> depPolicyIterator = depPolicies.iterator();
        while (depPolicyIterator.hasNext()) {
            DeploymentPolicy depPolicy = depPolicyIterator.next();
            try {
            		addDeploymentPolicyToInformationModel(depPolicy);
            }
            catch (InvalidPolicyException e) {
            	log.warn("Unable to load Deployment policy to information Model ["+ depPolicy.getTenantId() + "]");
	    	}
        }
    }

    /**
     * Deploys the specified Autoscaling policy - Adds to information model and persists
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public boolean deployAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
    	if(StringUtils.isEmpty(policy.getId())) {
            throw new AutoScalerException("AutoScaling policy id can not be empty");
        }
        this.addASPolicyToInformationModel(policy);
       	        
       	RegistryManager.getInstance(CarbonContext.getThreadLocalCarbonContext().getTenantId()).persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("AutoScaling policy is deployed successfully: [id] %s", policy.getId()));
        }
        return true;
    }
    
    /**
     * Adds the specified Autoscaling policy to Information model
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public void addASPolicyToInformationModel(AutoscalePolicy asPolicy) throws InvalidPolicyException {
    	
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	    	  
    	if(asPolicy.getIsPublic()) {
        	addASPolicyToSpecificContainer(asPolicy, StratosConstants.PUBLIC_DEFINITION);
        }
    	else {
    		addASPolicyToSpecificContainer(asPolicy, tenantId);
    	}
    }
    
    /**
     * Adds the as policy to specific container.
     *
     * @param asPolicy the as policy
     * @param tenantId the tenant id
     * @throws InvalidPolicyException the invalid policy exception
     */
    private void addASPolicyToSpecificContainer(AutoscalePolicy asPolicy, int tenantId) throws InvalidPolicyException {
    	
    	Map<String, AutoscalePolicy> policies;
    	if(!autoscalePolicyListMap.containsKey(tenantId)) {
    		policies = new HashMap<String, AutoscalePolicy>();
    		autoscalePolicyListMap.put(tenantId, policies);
    	}
    	else {
    		policies = autoscalePolicyListMap.get(tenantId);
    	}
    	
        if (!policies.containsKey(asPolicy.getId())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding policy :" + asPolicy.getId());
            }
            policies.put(asPolicy.getId(), asPolicy);
        } else {
        	String errMsg = "Specified policy [" + asPolicy.getId() + "] already exists";
        	log.error(errMsg);
            throw new InvalidPolicyException(errMsg);
        }
    }
    
    /**
     * Removes the specified Autoscaling policy
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public void undeployAutoscalePolicy(String policy) throws InvalidPolicyException { 
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	AutoscalePolicy policyToDelete = getAutoscalePolicy(policy);
    	
    	if ((policyToDelete == null) || (policyToDelete.getTenantId() != tenantId))
    		throw new InvalidPolicyException("No such policy [" + policy + "] exists");
    	
    	if (log.isDebugEnabled()) {
            log.debug("Removing policy :" + policy);
        }
        
    	RegistryManager.getInstance(tenantId).removeAutoscalerPolicy(policyToDelete);
    	if (policyToDelete.getIsPublic()) {
    		(autoscalePolicyListMap.get(StratosConstants.PUBLIC_DEFINITION)).remove(policy);
    	}
    	else {
    		(autoscalePolicyListMap.get(tenantId)).remove(policy);
    	}
    	
    }
    
    /**
     * Returns an array of the Autoscaling policies contained in this manager.
     *
     * @return the autoscale policy list
     */
    public AutoscalePolicy[] getAutoscalePolicyList() {        
    	List<AutoscalePolicy> policyList = new ArrayList<AutoscalePolicy>();
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	
    	if (autoscalePolicyListMap.containsKey(tenantId))
    		policyList.addAll(autoscalePolicyListMap.get(tenantId).values());
		
    	if (autoscalePolicyListMap.containsKey(StratosConstants.PUBLIC_DEFINITION))
    		policyList.addAll(autoscalePolicyListMap.get(StratosConstants.PUBLIC_DEFINITION).values());
    	
    	return policyList.toArray(new AutoscalePolicy[0]);
    }

    /**
     * Returns the Autoscaling policy to which the specified id is mapped or null.
     *
     * @param id the id
     * @return the autoscale policy
     */
    public AutoscalePolicy getAutoscalePolicy(String id) {
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	if (autoscalePolicyListMap.containsKey(StratosConstants.PUBLIC_DEFINITION)) {
        	if ((autoscalePolicyListMap.get(StratosConstants.PUBLIC_DEFINITION)).containsKey(id))
        		return (autoscalePolicyListMap.get(StratosConstants.PUBLIC_DEFINITION)).get(id);
        }
    	
    	if (autoscalePolicyListMap.containsKey(tenantId)) {
    		if ((autoscalePolicyListMap.get(tenantId)).containsKey(id))
    			return (autoscalePolicyListMap.get(tenantId)).get(id);
        }
        
    	return null;
    }
    
    /**
     * Deletes the Autoscaling policies for specified tenantId or null.
     *
     * @param tenantId the tenant id
     */
    public void removeASPoliciesFromInformationModel(int tenantId) {
    	autoscalePolicyListMap.remove(tenantId);
    }
    
    /**
     * Deploys the specified Deployment policy - Adds to information model and persists
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public boolean deployDeploymentPolicy(DeploymentPolicy policy) throws InvalidPolicyException {
    	if (StringUtils.isEmpty(policy.getId())) {
            throw new AutoScalerException("Deploying policy id can not be empty");
        }
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Deploying deployment policy: [id] %s", policy.getId()));
            }
            fillPartitions(policy);
        } catch (InvalidPartitionException e) {
        	log.error(e);
            throw new InvalidPolicyException(String.format("Deployment policy is invalid: [id] %s", policy.getId()), e);
        }
        
        this.addDeploymentPolicyToInformationModel(policy);
        RegistryManager.getInstance(CarbonContext.getThreadLocalCarbonContext().getTenantId()).persistDeploymentPolicy(policy);

        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is deployed successfully: [id] %s", policy.getId()));
        }
        return true;
    }
    
    /**
     * Adds the specified Deployment policy to Information model
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public void addDeploymentPolicyToInformationModel(DeploymentPolicy policy) throws InvalidPolicyException {
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	
        if (policy.getIsPublic()) {
        	addDeploymentPolicyToSpecificContainer(policy, StratosConstants.PUBLIC_DEFINITION);
        }
        else {
        	addDeploymentPolicyToSpecificContainer(policy, tenantId);
        }
    }
    
    /**
     * Adds the deployment policy to specific container.
     *
     * @param deploymentPolicy the deployment policy
     * @param tenantId the tenant id
     * @throws InvalidPolicyException the invalid policy exception
     */
    private void addDeploymentPolicyToSpecificContainer(DeploymentPolicy deploymentPolicy, int tenantId) throws InvalidPolicyException {
    	
    	Map<String, DeploymentPolicy> policies;
    	if (!deploymentPolicyListMap.containsKey(tenantId)) {
    		policies = new HashMap<String, DeploymentPolicy>();
    		deploymentPolicyListMap.put(tenantId, policies);
    	}
    	else {
    		policies = deploymentPolicyListMap.get(tenantId);
    	}
    	
        if (!policies.containsKey(deploymentPolicy.getId())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding policy :" + deploymentPolicy.getId());
            }
            policies.put(deploymentPolicy.getId(), deploymentPolicy);
        } else {
        	String errMsg = "Specified policy [" + deploymentPolicy.getId() + "] already exists";
        	log.error(errMsg);
            throw new InvalidPolicyException(errMsg);
        }
    }


    private void fillPartitions(DeploymentPolicy deploymentPolicy) throws InvalidPartitionException {
        PartitionManager partitionMgr = PartitionManager.getInstance();
        for (Partition partition : deploymentPolicy.getAllPartitions()) {
            String partitionId = partition.getId();
            if ((partitionId == null) || (!partitionMgr.partitionExist(partitionId))) {
                String msg = "Could not find partition: [id] " + partitionId + ". " +
                        "Please deploy the partitions before deploying the deployment policies.";                
                throw new InvalidPartitionException(msg);
            }
            
            fillPartition(partition, PartitionManager.getInstance().getPartitionById(partitionId));
        }
    }

    private static void fillPartition(Partition destPartition, Partition srcPartition) {
        if(srcPartition.getProvider() == null)        	
            throw new RuntimeException("Provider is not set in the deployed partition");

        if (log.isDebugEnabled()) {
            log.debug(String.format("Setting provider for partition: [id] %s [provider] %s", destPartition.getId(), srcPartition.getProvider()));
        }
        destPartition.setProvider(srcPartition.getProvider());

        if (log.isDebugEnabled()) {
            log.debug(String.format("Setting properties for partition: [id] %s [properties] %s", destPartition.getId(), srcPartition.getProperties()));
        }
        destPartition.setProperties(srcPartition.getProperties());
    }

    /**
     * Removes the specified Deployment policy
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public void undeployDeploymentPolicy(String policy) throws InvalidPolicyException {
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	DeploymentPolicy policyToDelete = getDeploymentPolicy(policy);
    	
    	if ((policyToDelete == null) || (policyToDelete.getTenantId() != tenantId))
    		throw new InvalidPolicyException("No such policy [" + policy + "] exists");
    	
    	if (log.isDebugEnabled()) {
            log.debug("Removing policy :" + policy);
        }
        
    	RegistryManager.getInstance(tenantId).removeDeploymentPolicy(policyToDelete);
    	if (policyToDelete.getIsPublic()) {
    		(deploymentPolicyListMap.get(StratosConstants.PUBLIC_DEFINITION)).remove(policy);
    	}
    	else {
    	       (deploymentPolicyListMap.get(tenantId)).remove(policy);
    	}
    }

    /**
     * Returns an array of the Deployment policies contained in this manager.
     *
     * @return the deployment policy list
     */
    public DeploymentPolicy[] getDeploymentPolicyList() {        
    	List<DeploymentPolicy> policyList = new ArrayList<DeploymentPolicy>();
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	
    	if (deploymentPolicyListMap.containsKey(tenantId))
    			policyList.addAll(deploymentPolicyListMap.get(CarbonContext.getThreadLocalCarbonContext().getTenantId()).values());
		
    	if (deploymentPolicyListMap.containsKey(StratosConstants.PUBLIC_DEFINITION))
    			policyList.addAll(deploymentPolicyListMap.get(StratosConstants.PUBLIC_DEFINITION).values());
    	
    	return policyList.toArray(new DeploymentPolicy[0]);
    }

    /**
     * Returns the deployment policy to which the specified id is mapped or null.
     *
     * @param id the id
     * @return the deployment policy
     */
    public DeploymentPolicy getDeploymentPolicy(String id) {
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	if (deploymentPolicyListMap.containsKey(StratosConstants.PUBLIC_DEFINITION)) {
    		if ((deploymentPolicyListMap.get(StratosConstants.PUBLIC_DEFINITION)).containsKey(id))
    			return (deploymentPolicyListMap.get(StratosConstants.PUBLIC_DEFINITION)).get(id);
        }
    	
    	if (deploymentPolicyListMap.containsKey(tenantId)) {
    		if ((deploymentPolicyListMap.get(tenantId)).containsKey(id))
    			return (deploymentPolicyListMap.get(tenantId)).get(id);
        }
    	
    	return null;
    }
    
    /**
     * Deletes the deployment policies for specified tenantId .
     *
     * @param tenantId the tenant id
     */
    public void removeDeploymentPoliciesFromInformationModel(int tenantId) {
    	deploymentPolicyListMap.remove(tenantId);
    }

}