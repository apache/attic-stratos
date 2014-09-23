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

package org.apache.stratos.autoscaler.partition;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.common.constants.StratosConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The model class for managing Partitions.
 */
public class PartitionManager {

private static final Log log = LogFactory.getLog(PartitionManager.class);
	
	// Partitions against partitionID
	private static Map<Integer, Map<String,Partition>> partitions = new HashMap<Integer, Map<String, Partition>>();
	
	/*
	 * Key - network partition id
	 * Value - reference to NetworkPartition
	 */
	private Map<String, NetworkPartitionLbHolder> networkPartitionLbHolders;

	private static class Holder {
        static final PartitionManager INSTANCE = new PartitionManager();
    }
	
	public static PartitionManager getInstance(){
	    return Holder.INSTANCE;
	}
	
	private PartitionManager(){
        networkPartitionLbHolders = new HashMap<String, NetworkPartitionLbHolder>();
	}
	
    // Checks whether a given tenant's policies have been added to memory model
    public boolean isTenantPolicyDetailsInInformationModel(int containerId){
    	return (partitions.containsKey(containerId));
    }
	
	public boolean partitionExist(String partitionId){
		int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        if(partitions.containsKey(tenantId)){
    		return (partitions.get(tenantId)).containsKey(partitionId);
        }
        else if(partitions.containsKey(StratosConstants.PUBLIC_DEFINITION)){
        	return (partitions.get(StratosConstants.PUBLIC_DEFINITION)).containsKey(partitionId);
        }
        return false;
	}
	
	/*
	 * Deploy a new partition to Auto Scaler.
	 */
    public boolean addNewPartition(Partition partition) throws InvalidPartitionException {
        if (StringUtils.isEmpty(partition.getId())){
            throw new InvalidPartitionException("Partition id can not be empty");
        }
        if (this.partitionExist(partition.getId())) {
            throw new InvalidPartitionException(String.format("Partition already exist in partition manager: [id] %s", partition.getId()));
        }
        if (null == partition.getProvider()) {
            throw new InvalidPartitionException("Mandatory field provider has not be set for partition " + partition.getId());
        }
        try {
            validatePartitionViaCloudController(partition);
            RegistryManager.getInstance().persistPartition(partition);
            addPartitionToInformationModel(partition);
            if (log.isInfoEnabled()) {
                log.info(String.format("Partition is deployed successfully: [id] %s", partition.getId()));
            }
            return true;
        } catch (Exception e) {
            throw new InvalidPartitionException(e.getMessage(), e);
        }
    }

    public void loadPartitionsToInformationModel(){
    	List<Partition> partitions = RegistryManager.getInstance().retrievePartitions();
        Iterator<Partition> partitionIterator = partitions.iterator();
        while (partitionIterator.hasNext()) {
            Partition partition = partitionIterator.next();
            try{
            	addPartitionToInformationModel(partition);
            }
            catch(InvalidPolicyException e){
            	// ignore and move on
            }
        }
    }
    
    public void removePartitionsFromInformationModel(int tenantId){
    	partitions.remove(tenantId);
    }
       
    public void loadNetworkPartitionsToInformationModel(){
    	List<NetworkPartitionLbHolder> nwPartitionHolders = RegistryManager.getInstance().retrieveNetworkPartitionLbHolders();
        Iterator<NetworkPartitionLbHolder> nwPartitionIterator = nwPartitionHolders.iterator();
        while (nwPartitionIterator.hasNext()) {
            NetworkPartitionLbHolder nwPartition = nwPartitionIterator.next();
            PartitionManager.getInstance().addNetworkPartitionLbHolder(nwPartition);
        }
    }

    public void addPartitionToInformationModel(Partition partition) throws InvalidPolicyException {
    	int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    	        
        if(partition.getIsPublic()){
        	addPartitionToSpecificContainer(partition, StratosConstants.PUBLIC_DEFINITION);
        }
        else{
        	addPartitionToSpecificContainer(partition, tenantId);
        }
	}
    
    private void addPartitionToSpecificContainer(Partition partition, int containerId) throws InvalidPolicyException {
    	
    	Map<String, Partition> partitionDefinitions;
    	if(!partitions.containsKey(containerId))
    	{
    		partitionDefinitions = new HashMap<String, Partition>();
    		partitions.put(containerId, partitionDefinitions);
    	}
    	else {
    		partitionDefinitions = partitions.get(containerId);
    	}
    	
        if (!partitionDefinitions.containsKey(partition.getId())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding policy :" + partition.getId());
            }
            partitionDefinitions.put(partition.getId(), partition);
        } else {
        	String errMsg = "Specified policy [" + partition.getId() + "] already exists";
        	log.error(errMsg);
            throw new InvalidPolicyException(errMsg);
        }
    }

	public NetworkPartitionLbHolder getNetworkPartitionLbHolder(String networkPartitionId) {
	    return this.networkPartitionLbHolders.get(networkPartitionId);
	}

    public Partition getPartitionById(String partitionId){
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        if(partitions.containsKey(tenantId)){
    		return (partitions.get(tenantId)).get(partitionId);
        }
        else if(partitions.containsKey(StratosConstants.PUBLIC_DEFINITION)){
        	return (partitions.get(StratosConstants.PUBLIC_DEFINITION)).get(partitionId);
        }
    	
    	return null;
	}
	
	public Partition[] getAllPartitions(){
		System.out.println("Tenant ID from PartitionManager-getAllAvailablePartitions: "+ CarbonContext.getThreadLocalCarbonContext().getTenantId());
		List<Partition> policyList = new ArrayList<Partition>();
    	int t = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
		
    	if(partitions.containsKey(t))
    		policyList.addAll(partitions.get(CarbonContext.getThreadLocalCarbonContext().getTenantId()).values());
		
    	if(partitions.containsKey(StratosConstants.PUBLIC_DEFINITION))
		policyList.addAll(partitions.get(StratosConstants.PUBLIC_DEFINITION).values());
    	
    	return policyList.toArray(new Partition[0]);
	}
	
	public boolean validatePartitionViaCloudController(Partition partition) throws PartitionValidationException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Validating partition via cloud controller: [id] %s", partition.getId()));
        }
		return CloudControllerClient.getInstance().validatePartition(partition);
	}
	
	public List<NetworkPartitionLbHolder> getNetworkPartitionLbHolders(DeploymentPolicy depPolicy) {
		List<NetworkPartitionLbHolder> lbHolders = new ArrayList<NetworkPartitionLbHolder>();
		for(PartitionGroup partitionGroup: depPolicy.getPartitionGroups()){
            String id = partitionGroup.getId();
            NetworkPartitionLbHolder entry = networkPartitionLbHolders.get(id);
            if(entry != null) {
            	lbHolders.add(entry);
            }
		}
		return lbHolders;
	}

    public void deployNewNetworkPartitions(DeploymentPolicy depPolicy) {
        for(PartitionGroup partitionGroup: depPolicy.getPartitionGroups()){
            String id = partitionGroup.getId();
            if (!networkPartitionLbHolders.containsKey(id)) {
                NetworkPartitionLbHolder networkPartitionLbHolder =
                        new NetworkPartitionLbHolder(id);
                addNetworkPartitionLbHolder(networkPartitionLbHolder);
                RegistryManager.getInstance().persistNetworkPartitionIbHolder(networkPartitionLbHolder);
            }
        }
    }
    
    public void undeployNetworkPartitions(DeploymentPolicy depPolicy) {
        for(PartitionGroup partitionGroup: depPolicy.getPartitionGroups()){
            String id = partitionGroup.getId();
            if (networkPartitionLbHolders.containsKey(id)) {                
                NetworkPartitionLbHolder netPartCtx = this.getNetworkPartitionLbHolder(id);
                // remove from information model
                this.removeNetworkPartitionLbHolder(netPartCtx);
                //remove from the registry
                RegistryManager.getInstance().removeNetworkPartition(this.getNetworkPartitionLbHolder(id).getNetworkPartitionId());
            }else{
            	String errMsg = "Network partition context not found for policy " + depPolicy;
            	log.error(errMsg);
            	throw new AutoScalerException(errMsg);
            }
        }
    }
    
    private void removeNetworkPartitionLbHolder(NetworkPartitionLbHolder nwPartLbHolder) {
    	 networkPartitionLbHolders.remove(nwPartLbHolder.getNetworkPartitionId());
	}

	public void addNetworkPartitionLbHolder(NetworkPartitionLbHolder nwPartLbHolder) {
        networkPartitionLbHolders.put(nwPartLbHolder.getNetworkPartitionId(), nwPartLbHolder);
    }

}
