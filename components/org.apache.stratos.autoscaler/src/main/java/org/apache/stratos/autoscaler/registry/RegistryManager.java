package org.apache.stratos.autoscaler.registry;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.Deserializer;
import org.apache.stratos.autoscaler.util.Serializer;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.common.kubernetes.KubernetesGroup;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.RegistryService;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegistryManager {

    private final static Log log = LogFactory.getLog(RegistryManager.class);
    private static RegistryService registryService;
    private static RegistryManager registryManager;
    private static Registry registry;
   
    /**
     * Gets the single instance of RegistryManager where the registry is for the current tenant.
     *
     * @param tenantId the tenant id
     * @return single instance of RegistryManager
     */
    public static RegistryManager getInstance(int tenantId) {
    	try {
    		registryService = ServiceReferenceHolder.getInstance().getRegistry();
    		registry = (Registry) registryService.getGovernanceSystemRegistry(tenantId);
    	}
    	catch(RegistryException e){
    		String msg = "Failed when retrieving Governance System Registry.";
            log.error(msg, e);
            throw new AutoScalerException(msg, e);
    	} 
        
        synchronized (RegistryManager.class) {
            if (registryManager == null) {
                if (registryService == null) {
                    // log.warn("Registry Service is null. Hence unable to fetch data from registry.");
                    return registryManager;
                }
                registryManager = new RegistryManager();
            }
        }
        return registryManager;
    }

    private RegistryManager() {
        try {
            if (!registry.resourceExists(AutoScalerConstants.AUTOSCALER_RESOURCE)) {
            	registry.put(AutoScalerConstants.AUTOSCALER_RESOURCE,
            			registry.newCollection());
            }
        } catch (RegistryException e) {
            String msg =
                    "Failed to create the registry resource " +
                            AutoScalerConstants.AUTOSCALER_RESOURCE;
            log.error(msg, e);
            throw new AutoScalerException(msg, e);
        }
    }

    /**
     * Persist an object in the local registry.
     *
     * @param dataObj      object to be persisted.
     * @param resourcePath resource path to be persisted.
     */
    private void persist(Object dataObj, String resourcePath) throws AutoScalerException {

        try {
        	registry.beginTransaction();

            Resource nodeResource = registry.newResource();
            nodeResource.setContent(Serializer.serializeToByteArray(dataObj));

            registry.put(resourcePath, nodeResource);
            registry.commitTransaction();
        } catch (Exception e) {
            try {
            	registry.rollbackTransaction();
            } catch (RegistryException e1) {
                if (log.isErrorEnabled()) {
                    log.error("Could not rollback transaction", e);
                }
            }
            throw new AutoScalerException("Could not persist data in registry", e);
        }
    }

    public void persistPartition(Partition partition) {
    	String resourcePath;
    	if (!partition.getIsPublic()) {
	        resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.PARTITION_RESOURCE + "/" + AutoScalerConstants.TENANT_RESOURCE + "/" + partition.getId();
    	} else {
    		resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.PARTITION_RESOURCE + "/" + AutoScalerConstants.PUBLIC_RESOURCE + "/" + partition.getId();
    	}
        persist(partition, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Partition written to registry: [id] %s [provider] %s [min] %d [max] %d",
                    partition.getId(), partition.getProvider(), partition.getPartitionMin(), partition.getPartitionMax()));
        }
    }

    public void persistNetworkPartitionIbHolder(NetworkPartitionLbHolder nwPartitionLbHolder) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants
                .NETWORK_PARTITION_LB_HOLDER_RESOURCE + "/" + nwPartitionLbHolder.getNetworkPartitionId();
        persist(nwPartitionLbHolder, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug("NetworkPartitionContext written to registry: " + nwPartitionLbHolder.toString());
        }
    }

    public void persistAutoscalerPolicy(AutoscalePolicy autoscalePolicy) {
    	String resourcePath;
    	if (!autoscalePolicy.getIsPublic()) {
	        resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + "/" + AutoScalerConstants.TENANT_RESOURCE + "/" + autoscalePolicy.getId();
    	} else {
    		resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + "/" + AutoScalerConstants.PUBLIC_RESOURCE + "/" + autoscalePolicy.getId();
    	}
        persist(autoscalePolicy, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Autoscaler policy written to registry: [id] %s [name] %s [description] %s",
                    autoscalePolicy.getId(), autoscalePolicy.getDisplayName(), autoscalePolicy.getDescription()));
        }
    }

    public void persistDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
    	String resourcePath;
    	if (!deploymentPolicy.getIsPublic()) {
	        resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/" + AutoScalerConstants.TENANT_RESOURCE + "/" + deploymentPolicy.getId();
    	} else {
    		resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/" + AutoScalerConstants.PUBLIC_RESOURCE + "/" + deploymentPolicy.getId();
    	}
        persist(deploymentPolicy, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(deploymentPolicy.toString());
        }
    }

    private Object retrieve(String resourcePath) {
        try {
            Resource resource = registry.get(resourcePath);

            return resource.getContent();

        } catch (ResourceNotFoundException ignore) {
            // this means, we've never persisted info in registry
            return null;
        } catch (RegistryException e) {
            String msg = "Failed to retrieve data from registry.";
            log.error(msg, e);
            throw new AutoScalerException(msg, e);
        }
    }

    public List<Partition> retrievePartitions() {
    	List<Partition> partitionList = new ArrayList<Partition>();
        RegistryManager registryManager = RegistryManager.getInstance(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.PARTITION_RESOURCE + AutoScalerConstants.TENANT_RESOURCE);
        String[] publicPartitionsResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.PARTITION_RESOURCE + AutoScalerConstants.PUBLIC_RESOURCE);
        
        ArrayList<String> allPartitions = new ArrayList<String>();
        if (partitionsResourceList != null)
        	allPartitions.addAll(Arrays.asList(partitionsResourceList));
        if (publicPartitionsResourceList != null)
        	allPartitions.addAll(Arrays.asList(publicPartitionsResourceList));

        if (allPartitions != null) {
            Partition partition;
            for (String resourcePath : allPartitions) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {

                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof Partition) {
                            partition = (Partition) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Partition read from registry: [id] %s [provider] %s [min] %d [max] %d",
                                        partition.getId(), partition.getProvider(), partition.getPartitionMin(), partition.getPartitionMax()));
                            }
                            partitionList.add(partition);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical partitions will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return partitionList;
    }

    public List<NetworkPartitionLbHolder> retrieveNetworkPartitionLbHolders() {
        List<NetworkPartitionLbHolder> nwPartitionLbHolderList = new ArrayList<NetworkPartitionLbHolder>();
        RegistryManager registryManager = RegistryManager.getInstance(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE +
                AutoScalerConstants.NETWORK_PARTITION_LB_HOLDER_RESOURCE);

        if (partitionsResourceList != null) {
            NetworkPartitionLbHolder nwPartitionLbHolder;
            for (String resourcePath : partitionsResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {

                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof NetworkPartitionLbHolder) {
                            nwPartitionLbHolder = (NetworkPartitionLbHolder) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("NetworkPartitionLbHolder read from registry: " + nwPartitionLbHolder.toString()));
                            }
                            nwPartitionLbHolderList.add(nwPartitionLbHolder);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical NetworkPartitionLbHolder will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return nwPartitionLbHolderList;
    }

    public List<AutoscalePolicy> retrieveASPolicies() {
    	List<AutoscalePolicy> asPolicyList = new ArrayList<AutoscalePolicy>();
        RegistryManager registryManager = RegistryManager.getInstance(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        String[] asPolicyResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + AutoScalerConstants.TENANT_RESOURCE);
        String[] publicAsPolicyResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + AutoScalerConstants.PUBLIC_RESOURCE);
        
        ArrayList<String> allAsPolicies = new ArrayList<String>();
        if (asPolicyResourceList != null)
        	allAsPolicies.addAll(Arrays.asList(asPolicyResourceList));
        if (publicAsPolicyResourceList != null)
        	allAsPolicies.addAll(Arrays.asList(publicAsPolicyResourceList));
        
        if (allAsPolicies != null) {
            AutoscalePolicy asPolicy;
            for (String resourcePath : allAsPolicies) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {
                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof AutoscalePolicy) {
                            asPolicy = (AutoscalePolicy) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Autoscaler policy read from registry: [id] %s [name] %s [description] %s",
                                        asPolicy.getId(), asPolicy.getDisplayName(), asPolicy.getDescription()));
                            }
                            asPolicyList.add(asPolicy);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical autoscaler policies will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return asPolicyList;
    }

    public List<DeploymentPolicy> retrieveDeploymentPolicies() {
    	List<DeploymentPolicy> depPolicyList = new ArrayList<DeploymentPolicy>();
        RegistryManager registryManager = RegistryManager.getInstance(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        String[] depPolicyResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE + AutoScalerConstants.TENANT_RESOURCE);
        String[] publicDepPolicyResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE + AutoScalerConstants.PUBLIC_RESOURCE);
        
        ArrayList<String> allDepPolicies = new ArrayList<String>();
        if (depPolicyResourceList != null)
        	allDepPolicies.addAll(Arrays.asList(depPolicyResourceList));
        if (publicDepPolicyResourceList != null)
        	allDepPolicies.addAll(Arrays.asList(publicDepPolicyResourceList));

        if (allDepPolicies != null) {
            DeploymentPolicy depPolicy;
            for (String resourcePath : allDepPolicies) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {
                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof DeploymentPolicy) {
                            depPolicy = (DeploymentPolicy) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(depPolicy.toString());
                            }
                            depPolicyList.add(depPolicy);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical deployment policies will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return depPolicyList;
    }

    public void removeAutoscalerPolicy(AutoscalePolicy autoscalePolicy) {
    	String resourcePath;
		if(!autoscalePolicy.getIsPublic()) {
			resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + "/" + AutoScalerConstants.TENANT_RESOURCE + "/" + autoscalePolicy.getId();
		}
		else {
			resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + "/" + AutoScalerConstants.PUBLIC_RESOURCE + "/" +autoscalePolicy.getId();
		}
    	
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Autoscaler policy deleted from registry: [id] %s [name] %s [description] %s",
                    autoscalePolicy.getId(), autoscalePolicy.getDisplayName(), autoscalePolicy.getDescription()));
        }

    }

    public void removeDeploymentPolicy(DeploymentPolicy depPolicy) {
    	String resourcePath;
		if(!depPolicy.getIsPublic()) {
			resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/" + AutoScalerConstants.TENANT_RESOURCE + "/" + depPolicy.getId();
		}
		else {
			resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/" + AutoScalerConstants.PUBLIC_RESOURCE + "/" +depPolicy.getId();
		}
    	
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Deployment policy deleted from registry: [id] %s",
                    depPolicy.getId()));
        }
    }

    public void removeNetworkPartition(String networkPartition) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.NETWORK_PARTITION_LB_HOLDER_RESOURCE;
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Network partition deleted from registry: [id] %s",
                    networkPartition));
        }
    }


    private void delete(String resourcePath) {
        try {
        	registry.beginTransaction();
        	registry.delete(resourcePath);
        	registry.commitTransaction();
        } catch (RegistryException e) {
            try {
            	registry.rollbackTransaction();
            } catch (RegistryException e1) {
                if (log.isErrorEnabled()) {
                    log.error("Could not rollback transaction", e);
                }
            }
            log.error("Could not delete resource at " + resourcePath);
            throw new AutoScalerException("Could not delete data in registry at " + resourcePath, e);
        }

    }

    public void persistKubernetesGroup(KubernetesGroup kubernetesGroup) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.KUBERNETES_RESOURCE
                + "/" + kubernetesGroup.getGroupId();
        persist(kubernetesGroup, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("KubernetesGroup written to registry: [id] %s ", kubernetesGroup.getGroupId()));
        }
    }

    public List<KubernetesGroup> retrieveKubernetesGroups() {
        List<KubernetesGroup> kubernetesGroupList = new ArrayList<KubernetesGroup>();
        RegistryManager registryManager = RegistryManager.getInstance(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        String[] kubernetesGroupResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.KUBERNETES_RESOURCE);

        if (kubernetesGroupResourceList != null) {
            KubernetesGroup kubernetesGroup;
            for (String resourcePath : kubernetesGroupResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {
                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof KubernetesGroup) {
                            kubernetesGroup = (KubernetesGroup) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(kubernetesGroup.toString());
                            }
                            kubernetesGroupList.add(kubernetesGroup);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical Kubernetes groups deployments will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return kubernetesGroupList;
    }

    public void removeKubernetesGroup(KubernetesGroup kubernetesGroup) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.KUBERNETES_RESOURCE + "/" + kubernetesGroup.getGroupId();
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Kubernetes group deleted from registry: [id] %s", kubernetesGroup.getGroupId()));
        }
    }
}
