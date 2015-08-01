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

package org.apache.stratos.gce.extension.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.gce.extension.config.GCEContext;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All the GCE API calls will be done using this class
 */

public class GCEOperations {
    private static final Log log = LogFactory.getLog(GCEOperations.class);

    //project related
    private static final String PROJECT_NAME = GCEContext.getInstance().getProjectName();
    private static final String PROJECT_ID = GCEContext.getInstance().getProjectID();
    private static final String REGION_NAME = GCEContext.getInstance().getRegionName();

    //auth
    private static final String KEY_FILE_PATH = GCEContext.getInstance().getKeyFilePath();
    private static final String ACCOUNT_ID = GCEContext.getInstance().getGceAccountID();

    //health check
    private static final String HEALTH_CHECK_REQUEST_PATH = GCEContext.getInstance().getHealthCheckRequestPath();
    private static final String HEALTH_CHECK_PORT = GCEContext.getInstance().getHealthCheckPort();
    private static final String HEALTH_CHECK_TIME_OUT_SEC = GCEContext.getInstance().getHealthCheckTimeOutSec();
    private static final String HEALTH_CHECK_INTERVAL_SEC = GCEContext.getInstance().getHealthCheckIntervalSec();
    private static final String HEALTH_CHECK_UNHEALTHY_THRESHOLD = GCEContext.getInstance().getHealthCheckUnhealthyThreshold();
    private static final String HEALTH_CHECK_HEALTHY_THRESHOLD = GCEContext.getInstance().getHealthCheckHealthyThreshold();

    //a timeout for operation completion
    private static final int OPERATION_TIMEOUT = Integer.parseInt(GCEContext.getInstance().getOperationTimeout());
    static Compute compute;

    /**
     * Constructor for GCE Operations Class
     */
    public GCEOperations() throws IOException {
        buildComputeEngineObject();
    }

    /**
     * Authorize and build compute engine object
     */
    private void buildComputeEngineObject() throws IOException {

        try {

            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            GoogleCredential credential = new GoogleCredential.Builder().setTransport(httpTransport)
                    .setJsonFactory(jsonFactory)
                    .setServiceAccountId(ACCOUNT_ID)
                    .setServiceAccountScopes(Collections.singleton(ComputeScopes.COMPUTE))
                    .setServiceAccountPrivateKeyFromP12File(new File(KEY_FILE_PATH))
                    .build();

            // Create compute engine object
            compute = new Compute.Builder(
                    httpTransport, jsonFactory, null).setApplicationName(PROJECT_NAME)
                    .setHttpRequestInitializer(credential).build();
        } catch (GeneralSecurityException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not authenticate and build compute object");
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            log.fatal("Could not authenticate and build compute object");
            throw new IOException(e);
        }
    }

    /**
     * Get list of running instances in given project and zone.(This method can be used
     * to check whether a given instance is available or not in a given project
     * and zone)
     *
     * @return instanceList - list of instances(members in Stratos side)
     */
    public static InstanceList getInstanceList(String zoneName, String filter) {
        Compute.Instances.List instances;
        try {
            instances = compute.instances().
                    list(PROJECT_ID, zoneName).setFilter(filter);
            InstanceList instanceList = instances.execute();
            if (instanceList.getItems() == null) {
                log.info("No instances found for filter " + filter + " and zone " + zoneName);
                return null;
            } else {
                return instanceList;
            }
        } catch (IOException e) {
            log.warn("Could not get instance list from GCE", e);

        }
        return null;
    }


    /**
     * Get instance resource URL from given instance name
     *
     * @param instanceId - Id of the instance provided by IaaS
     * @return - Resource URL of the instance in IaaS
     */
    public static String getInstanceURLFromId(String instanceId) {

        String instanceURL;
        String zoneName = getZoneNameFromInstanceId(instanceId);
        String filter = "name eq " + getInstanceNameFromInstanceId(instanceId);
        //check whether the given instance is available
        InstanceList instanceList = getInstanceList(zoneName, filter);
        if (instanceList == null) {
            log.warn("No matching instance found for filter " + filter);
            return null;
        }
        for (Instance instance : instanceList.getItems()) {
            String instanceIdInIaaS = zoneName + "/" + instance.getName();
            if (instanceIdInIaaS.equals(instanceId)) {
                //instance is available
                //getInstance URL
                instanceURL = instance.getSelfLink();
                return instanceURL;
            }
        }
        log.warn("No matching instance found for filter " + filter);
        return null;
    }

    /**
     * Get a given health check resource URL
     *
     * @param healthCheckName - name of the health check in IaaS
     * @return - Resource URL of health check in IaaS side
     */
    public static String getHealthCheckURLFromName(String healthCheckName) {

        String healthCheckURL;

        //check whether the given instance is available
        HttpHealthCheckList healthCheckList;
        healthCheckList = getHealthCheckList();
        if (healthCheckList == null) {
            log.warn("Could not found health check " + healthCheckName + "since health check list is null");
            return null;
        }
        for (HttpHealthCheck httpHealthCheck : healthCheckList.getItems()) {
            if (httpHealthCheck.getName().equals(healthCheckName)) {
                //instance is available
                //getInstance URL
                healthCheckURL = httpHealthCheck.getSelfLink();
                return healthCheckURL;
            }
        }
        return null;
    }

    /**
     * Get list of health checks
     *
     * @return - list of health checks
     */
    private static HttpHealthCheckList getHealthCheckList() {

        Compute.HttpHealthChecks.List healthChecks;
        try {
            healthChecks = compute.httpHealthChecks().list(PROJECT_ID);
            HttpHealthCheckList healthCheckList = healthChecks.execute();
            if (healthCheckList.getItems() == null) {
                log.info("No health check found for specified project");
                return null;
            } else {
                return healthCheckList;
            }
        } catch (IOException e) {
            log.warn("Could not get health check list from GCE", e);
            return null;
        }
    }

    /**
     * Manually create the self link for an instance. This method is useful when the instance is deleted from IaaS
     *
     * @return - the self link of the instance
     */
    private static String createInstanceSelfLink(String instanceId) {
        return "https://www.googleapis.com/compute/v1/projects/" + PROJECT_ID +
                "/zones/" + getZoneNameFromInstanceId(instanceId) + "/instances/" + getInstanceNameFromInstanceId(instanceId);
    }

    /**
     * Creating a new target pool; name should be unique
     *
     * @param targetPoolName - name of the target pool in IaaS
     */
    public void createTargetPool(String targetPoolName, String healthCheckName) {

        log.info("Creating target pool: " + targetPoolName);

        TargetPool targetPool = new TargetPool();
        targetPool.setName(targetPoolName);
        List<String> httpHealthChecks = new ArrayList<String>();
        httpHealthChecks.add(getHealthCheckURLFromName(healthCheckName));
        targetPool.setHealthChecks(httpHealthChecks);

        try {
            Operation operation =
                    compute.targetPools().insert(PROJECT_ID, REGION_NAME, targetPool).execute();
            waitForRegionOperationCompletion(operation.getName());

        } catch (Exception e) {
            log.warn("Could not create target pool: " + targetPoolName + " " + e);
        }

    }

    /**
     * Deleting an exiting target pool in IaaS
     *
     * @param targetPoolName - Name of the target pool in IaaS
     */
    public void deleteTargetPool(String targetPoolName) {
        log.info("Deleting target pool: " + targetPoolName);
        try {
            Operation operation = compute.targetPools().delete(PROJECT_ID, REGION_NAME, targetPoolName).execute();
            waitForRegionOperationCompletion(operation.getName());

        } catch (Exception e) {
            log.warn("Could not delete target pool " + targetPoolName + " " + e);
        }
        log.info("Target pool: " + targetPoolName + " has been deleted");
    }

    /**
     * create forwarding rule by using given target pool and protocol
     *
     * @param forwardingRuleName - name of the forwarding rule in IaaS to be created
     * @param targetPoolName     - name of the target pool in IaaS which is already created
     * @param protocol           - The protocol which is used to forward trafic. Should be either TCP or UDP
     */

    public void createForwardingRule(String forwardingRuleName, String targetPoolName, String protocol,
                                     String portRange) {

        log.info("Creating a forwarding rule: " + forwardingRuleName);
        //Need to get target pool resource URL
        TargetPool targetPool = getTargetPool(targetPoolName);
        String targetPoolURL = targetPool.getSelfLink();
        ForwardingRule forwardingRule = new ForwardingRule();
        forwardingRule.setName(forwardingRuleName);
        forwardingRule.setTarget(targetPoolURL);
        forwardingRule.setIPProtocol(protocol);
        forwardingRule.setPortRange(portRange);
        try {
            Operation operation = compute.forwardingRules().insert(PROJECT_ID, REGION_NAME, forwardingRule).execute();
            waitForRegionOperationCompletion(operation.getName());
        } catch (Exception e) {
            log.warn("Could not create a forwarding rule " + forwardingRuleName + " " + e);
        }
        log.info("Created forwarding rule: " + forwardingRuleName);
    }

    /**
     * Deleting a existing forwarding rule in IaaS
     *
     * @param forwardingRuleName - Forwarding rule name in IaaS
     */
    public void deleteForwardingRule(String forwardingRuleName) {
        log.info("Deleting forwarding rule: " + forwardingRuleName);
        try {
            Operation operation = compute.forwardingRules().
                    delete(PROJECT_ID, REGION_NAME, forwardingRuleName).execute();
            waitForRegionOperationCompletion(operation.getName());
        } catch (Exception e) {
            log.warn("Could not delete forwarding rule " + forwardingRuleName + " " + e);
        }
        log.info("Deleted forwarding rule: " + forwardingRuleName);
    }

    /**
     * Check whether the given target pool is already exists in the given project and region.
     * Target pools are unique for regions, not for zones
     *
     * @param targetPoolName - target pool name in IaaS
     */
    public boolean isTargetPoolExists(String targetPoolName) {

        try {
            Compute.TargetPools.List targetPools = compute.targetPools().
                    list(PROJECT_ID, REGION_NAME);
            TargetPoolList targetPoolList = targetPools.execute();
            for (TargetPool targetPool : targetPoolList.getItems()) {
                if (targetPool.getName().equals(targetPoolName)) {
                    return true;
                }

            }

        } catch (IOException e) {
            log.warn("Could not check whether the target pool " + targetPoolName + " is exists or not " + e);
        }
        return false;
    }

    /**
     * Get a target pool already created in GCE
     *
     * @param targetPoolName - target pool name in IaaS
     * @return - target pool object
     */
    public TargetPool getTargetPool(String targetPoolName) {

        try {
            if (isTargetPoolExists(targetPoolName)) {
                return compute.targetPools().get(PROJECT_ID, REGION_NAME, targetPoolName).execute();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Requested Target Pool " + targetPoolName + " Is not Available");
                }
            }

            return null;

        } catch (IOException e) {
            log.warn("Could not get target pool " + targetPoolName + " " + e);
        }
        return null;

    }

    /**
     * Remove given set of instances from target pool
     *
     * @param targetPoolName   - target pool name from IaaS
     * @param instancesIdsList - List of instances to be removed from target pool
     */
    public void removeInstancesFromTargetPool(List<String> instancesIdsList, String targetPoolName) {
        log.info("Removing instances from target pool: " + targetPoolName);

        if (instancesIdsList.isEmpty()) {
            log.warn("Cannot remove instances to target pool. InstancesNamesList is empty.");
            return;
        }

        List<InstanceReference> instanceReferenceList = new ArrayList<InstanceReference>();

        //add instance to instance reference list, we should use the instance URL
        for (String instanceId : instancesIdsList) { //for all instances

            String instanceUrl = createInstanceSelfLink(instanceId);
            instanceReferenceList.add(new InstanceReference().
                    setInstance(instanceUrl));
        }

        //create target pools add instance request and set instance to it
        TargetPoolsRemoveInstanceRequest targetPoolsRemoveInstanceRequest = new
                TargetPoolsRemoveInstanceRequest();
        if (instanceReferenceList.size() == 0) {
            log.warn("Could not remove instances from target pool " + targetPoolName + " because instance reference list is null");
            return;
        }
        targetPoolsRemoveInstanceRequest.setInstances(instanceReferenceList);

        try {
            //execute
            Operation operation = compute.targetPools().removeInstance(PROJECT_ID, REGION_NAME,
                    targetPoolName, targetPoolsRemoveInstanceRequest).execute();

            waitForRegionOperationCompletion(operation.getName());

        } catch (Exception e) {
            log.warn("Could not remove instances from target pool " + targetPoolName + " " + e);
        }

        log.info("Removed instances from target pool: " + targetPoolName);
    }

    /**
     * Add a given list of instances to a target pool
     *
     * @param instancesIdsList - list of instance Ids
     * @param targetPoolName   - target pool name in IaaS
     */
    public void addInstancesToTargetPool(List<String> instancesIdsList, String targetPoolName) {

        log.info("Adding instances to target pool: " + targetPoolName);

        if (instancesIdsList.isEmpty()) {
            log.warn("Cannot add instances to target pool. InstancesNamesList is empty.");
            return;
        }

        List<InstanceReference> instanceReferenceList = new ArrayList<InstanceReference>();

        //add instance to instance reference list, we should use the instance URL
        for (String instanceId : instancesIdsList) { //for all instances

            String instanceUrl = getInstanceURLFromId(instanceId);
            if (instanceUrl != null) {
                instanceReferenceList.add(new InstanceReference().
                        setInstance(instanceUrl));
            }

        }

        //create target pools add instance request and set instance to it
        TargetPoolsAddInstanceRequest targetPoolsAddInstanceRequest = new
                TargetPoolsAddInstanceRequest();
        if (instanceReferenceList.size() == 0) {
            log.warn("Could not add instances to target pool " + targetPoolName + " because instance reference list is null");
            return;
        }

        targetPoolsAddInstanceRequest.setInstances(instanceReferenceList);

        try {
            //execute
            Operation operation = compute.targetPools().addInstance(PROJECT_ID, REGION_NAME,
                    targetPoolName, targetPoolsAddInstanceRequest).execute();
            waitForRegionOperationCompletion(operation.getName());

        } catch (Exception e) {
            log.warn("Could not add instance to target pool" + targetPoolName + " " + e);
        }
        log.info("Added instances to target pool: " + targetPoolName);
    }

    /**
     * Create a health check in IaaS
     *
     * @param healthCheckName - name of the health check to be created
     */
    public void createHealthCheck(String healthCheckName) {

        log.info("Creating health check: " + healthCheckName);

        HttpHealthCheck httpHealthCheck = new HttpHealthCheck();
        httpHealthCheck.setName(healthCheckName);
        httpHealthCheck.setRequestPath(HEALTH_CHECK_REQUEST_PATH);
        httpHealthCheck.setPort(Integer.parseInt(HEALTH_CHECK_PORT));
        httpHealthCheck.setTimeoutSec(Integer.parseInt(HEALTH_CHECK_TIME_OUT_SEC));
        httpHealthCheck.setCheckIntervalSec(Integer.parseInt(HEALTH_CHECK_INTERVAL_SEC));
        httpHealthCheck.setUnhealthyThreshold(Integer.parseInt(HEALTH_CHECK_UNHEALTHY_THRESHOLD));
        httpHealthCheck.setHealthyThreshold(Integer.parseInt(HEALTH_CHECK_HEALTHY_THRESHOLD));
        try {
            Operation operation = compute.httpHealthChecks().insert(PROJECT_ID, httpHealthCheck).execute();
            waitForGlobalOperationCompletion(operation.getName());

        } catch (Exception e) {
            log.error("Could not create health check " + healthCheckName + " " + e);
        }
        log.info("Created health check: " + healthCheckName);
    }

    public void deleteHealthCheck(String healthCheckName) {
        log.info("Deleting Health Check: " + healthCheckName);
        try {
            Operation operation = compute.httpHealthChecks().delete(PROJECT_ID, healthCheckName).execute();
            waitForGlobalOperationCompletion(operation.getName());

        } catch (Exception e) {
            log.warn("Could not get delete health check " + healthCheckName + " " + e);
        }
        log.info("Deleted Health Check: " + healthCheckName);
    }

    /**
     * Wait for a global operation completion. If the operation is related to whole project
     * that is a global operation
     *
     * @param operationName - name of the operation in IaaS
     */
    private void waitForGlobalOperationCompletion(String operationName) throws Exception {
        try {
            Thread.sleep(2000);
            int timeout = 0;
            while (true) {
                Operation operation = compute.globalOperations().get(PROJECT_ID, operationName).execute();
                if (operation.getStatus().equals("DONE")) {
                    return;
                }

                if (timeout >= OPERATION_TIMEOUT) {
                    log.warn("Timeout reached for operation " + operationName + ". Existing..");
                    return;
                }
                Thread.sleep(1000);
                timeout += 1000;
            }
        } catch (Exception e) {
            log.error("Could not wait for global operation completion " + operationName);
            throw new Exception(e);
        }
    }

    /**
     * Wait for a region operation completion. If the operation is related to a region that is a
     * region operation
     *
     * @param operationName - name of the region operation
     */

    private void waitForRegionOperationCompletion(String operationName) throws Exception {
        try {
            Thread.sleep(2000);
            int timeout = 0;
            while (true) {
                Operation operation = compute.regionOperations().get(PROJECT_ID, REGION_NAME, operationName).execute();

                if (operation.getStatus().equals("DONE")) {
                    return;
                }
                if (timeout >= OPERATION_TIMEOUT) {
                    log.warn("Timeout reached for operation " + operationName + ". Existing..");
                    return;
                }
                Thread.sleep(1000);
                timeout += 1000;
            }
        } catch (Exception e) {
            log.error("Could not wait for region operation completion " + operationName);
            throw new Exception(e);
        }
    }

    private static String getZoneNameFromInstanceId(String instanceId) {
        int lastIndexOfSlash = instanceId.lastIndexOf("/");
        return instanceId.substring(0, lastIndexOfSlash);
    }

    private static String getInstanceNameFromInstanceId(String instanceId) {
        int lastIndexOfSlash = instanceId.lastIndexOf("/");
        return instanceId.substring(lastIndexOfSlash + 1);
    }

}

