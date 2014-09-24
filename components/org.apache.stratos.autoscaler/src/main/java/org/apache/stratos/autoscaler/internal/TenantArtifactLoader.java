package org.apache.stratos.autoscaler.internal;

import java.util.Iterator;
import java.util.List;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;


public class TenantArtifactLoader extends AbstractAxis2ConfigurationContextObserver
{
	public void creatingConfigurationContext(int tenantId) {
		System.out.println("Just before creating");
    }

    public void createdConfigurationContext(ConfigurationContext configContext) {
    	System.out.println("Just after creating");
    	if(!checkIfArtifactsAreInSync()){
        	updateArtifacts();
        }
    }

    public void terminatingConfigurationContext(ConfigurationContext configCtx) {
    	System.out.println("Just before removing");	
    	clearArtifacts();
    }

    public void terminatedConfigurationContext(ConfigurationContext configCtx) {
    	System.out.println("Just after removing");
    }
    
    private boolean checkIfArtifactsAreInSync()
    {
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	System.out.println("************** Tenant ID : " + tenantId + "**************");
    	
    	// check whether tenant Id is present in inMemModels 
    	return (PolicyManager.getInstance().isTenantPolicyDetailsInInformationModel(tenantId) && 
    			PartitionManager.getInstance().isTenantPolicyDetailsInInformationModel(tenantId));
    }
    
    private void updateArtifacts()
    {
    	// Adding the registry stored partitions to the information model
        PartitionManager.getInstance().loadPartitionsToInformationModel();
        
        // Adding the network partitions stored in registry to the information model
        PartitionManager.getInstance().loadNetworkPartitionsToInformationModel();
        
        // Adding the registry stored autoscaling policies to the information model
        PolicyManager.getInstance().loadASPoliciesToInformationModel();
        
        // Adding the registry stored deployment policies to the information model
        PolicyManager.getInstance().loadDeploymentPoliciesToInformationModel();
    }
    
    private void clearArtifacts()
    {
    	int currentTenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	System.out.println("************** Tenant ID : " + currentTenantId + "**************");
    	
    	// Adding the registry stored partitions to the information model
        PartitionManager.getInstance().removePartitionsFromInformationModel(currentTenantId);
               
        // Adding the registry stored autoscaling policies to the information model
        PolicyManager.getInstance().removeASPoliciesFromInformationModel(currentTenantId);
        
        // Adding the registry stored deployment policies to the information model
        PolicyManager.getInstance().removeDeploymentPoliciesFromInformationModel(currentTenantId);
    }
}
