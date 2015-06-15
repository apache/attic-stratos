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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All the GCE API calls will be done using this class
 */

//TODO: exception handling
public class GCEOperations {

    private static final Log log = LogFactory.getLog(GCELoadBalancer.class);

    //project related
    private static final String PROJECT_NAME = GCEContext.getInstance().getProjectName();
    private static final String PROJECT_ID = GCEContext.getInstance().getProjectID();
    private static final String ZONE_NAME = GCEContext.getInstance().getZoneName();
    private static final String REGION_NAME = GCEContext.getInstance().getRegionName();

    //auth
    private static final String KEY_FILE_PATH = GCEContext.getInstance().getKeyFilePath();
    private static final String ACCOUNT_ID = GCEContext.getInstance().getGceAccountID();

    //health check
    private static final String HEALTH_CHECK_REQUEST_PATH = GCEContext.getInstance().getHealthCheckRequestPath();
    private static final String HEALTH_CHECK_PORT = GCEContext.getInstance().getHealthCheckPort();
    private static final String HEALTH_CHECK_TIME_OUT_SEC = GCEContext.getInstance().getHealthCheckTimeOutSec();
    private static final String HEALTH_CHECK_UNHEALTHY_THRESHOLD = GCEContext.getInstance().getHealthCheckUnhealthyThreshold();

    /**
     * Directory to store user credentials.
     */
    private static final String DATA_STORE_DIR = ".store/gce-extension";
    private static final String RUNNING_FILTER = "status eq RUNNING";
    private static final int OPERATION_TIMEOUT_MSILEC = 10000;


    static Compute compute;


    /**
     * Constructor for GCE Operations Class
     *
     * @throws LoadBalancerExtensionException
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public GCEOperations() throws LoadBalancerExtensionException,
            GeneralSecurityException, IOException {

        buildComputeEngineObject();

        //Calling following  methods from here only for testing purposes

    }

    /**
     * Authorize and build compute engine object
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private void buildComputeEngineObject() throws GeneralSecurityException, IOException {

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        DataStoreFactory dataStoreFactory = new FileDataStoreFactory(new
                File(System.getProperty("user.home"), DATA_STORE_DIR));
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


    }


    /**
     * Creating a new target pool; name should be unique
     *
     * @param targetPoolName - name of the target pool in IaaS
     */
    public void createTargetPool(String targetPoolName, String healthCheckName) {

        TargetPool targetPool = new TargetPool();
        targetPool.setName(targetPoolName);
        List<String> httpHealthChecks = new ArrayList<String>();
        httpHealthChecks.add(getHealthCheckURLFromName(healthCheckName));
        targetPool.setHealthChecks(httpHealthChecks);

        //TODO:REMOVE try catch
        try {
            Operation operation =
                    compute.targetPools().insert(PROJECT_ID, REGION_NAME, targetPool).execute();
            waitForRegionOperationCompletion(operation.getName());

        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("failed to create target pool: " + targetPoolName);
            }
            e.printStackTrace();
        }

    }

    /**
     * Deleting an exiting target pool in IaaS
     *
     * @param targetPoolName - Name of the target pool in IaaS
     */
    public void deleteTargetPool(String targetPoolName) {
        try {
            Operation operation = compute.targetPools().delete(PROJECT_ID, REGION_NAME, targetPoolName).execute();
            waitForRegionOperationCompletion(operation.getName());

            int timeout = 0;

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    /**
     * create forwarding rule by using given target pool and protocol
     *
     * @param forwardingRuleName - name of the forwarding rule in IaaS to be created
     * @param targetPoolName     - name of the target pool in IaaS which is already created
     * @param protocol           - The protocol which is used to forward trafic. Should be either TCP or UDP
     */

    public void createForwardingRule(String forwardingRuleName, String targetPoolName, String protocol, String portRange) {

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

            int timeout = 0;


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deleting a existing forwarding rule in IaaS
     *
     * @param forwardingRuleName - Forwarding rule name in IaaS
     */
    public void deleteForwardingRule(String forwardingRuleName) {
        try {
            Operation operation = compute.forwardingRules().
                    delete(PROJECT_ID, REGION_NAME, forwardingRuleName).execute();

            waitForGlobalOperationCompletion(operation.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Check whether the given target pool is already exists in the given project and region.
     * Target pools are unique for regions, not for zones
     *
     * @param targetPoolName - target pool name in IaaS
     */
    public boolean isTargetPoolExists(String targetPoolName) {

        //TODO: remove try catch
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
            if (log.isErrorEnabled()) {
                log.error("Error caused when checking for target pools");
            }
            e.printStackTrace();
        }
        return false;

    }

    /**
     * Check whether a given forwarding rule is exists or not in the IaaS
     *
     * @param forwardingRuleName - forwarding rule name in IaaS
     * @return
     */
    public boolean isForwardingRuleExists(String forwardingRuleName) {
        try {
            Compute.ForwardingRules.List forwardingRules = compute.forwardingRules().list(PROJECT_ID, REGION_NAME);
            ForwardingRuleList forwardingRuleList = forwardingRules.execute();
            for (ForwardingRule forwardingRule : forwardingRuleList.getItems()) {
                if (forwardingRule.getName().equals(forwardingRuleName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get a target pool already created in GCE
     *
     * @param targetPoolName - target pool name in IaaS
     * @return
     */
    public TargetPool getTargetPool(String targetPoolName) {
        //todo:remove try catch
        try {
            if (isTargetPoolExists(targetPoolName)) {
                return compute.targetPools().get(PROJECT_ID, REGION_NAME, targetPoolName).execute();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Requested Target Pool Is not Available");
                }
            }

            return null;

        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Exception caused when try to get target pool");
            }
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Add given set of instances to given target pool.
     *
     * @param targetPoolName        - target pool name in IaaS
     * @param instanceReferenceList - List of instances to be added to target pool
     */
    private void addInstancesToTargetPool(String targetPoolName, List<InstanceReference>
            instanceReferenceList) {


    }

    /**
     * Remove given set of instances from target pool
     *
     * @param targetPoolName        - target pool name from IaaS
     * @param instanceReferenceList - List of instances to be removed from target pool
     */
    public void removeInstancesFromTargetPool(String targetPoolName, List<InstanceReference>
            instanceReferenceList) {
        TargetPoolsRemoveInstanceRequest targetPoolsRemoveInstanceRequest
                = new TargetPoolsRemoveInstanceRequest();
        targetPoolsRemoveInstanceRequest.setInstances(instanceReferenceList);
        //TODO: remove try catch
        try {
            compute.targetPools().removeInstance(PROJECT_ID, REGION_NAME,
                    targetPoolName, targetPoolsRemoveInstanceRequest).execute();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Get list of running instances in given project and zone.(This method can be used
     * when we need to check whether a given instance is available or not in a given project
     * and zone)
     *
     * @return
     * @throws IOException
     */
    public static InstanceList getInstanceList() throws IOException {
        Compute.Instances.List instances = compute.instances().
                list(PROJECT_ID, ZONE_NAME).setFilter(RUNNING_FILTER);
        InstanceList instanceList = instances.execute();
        if (instanceList.getItems() == null) {
            log.info("No instances found for specified zone");
            return null;
        } else {
            return instanceList;
        }
    }

    public static String getInstanceURLFromName(String instanceName) {

        //todo:remove try catch
        String instanceURL;
        try {
            //check whether the given instance is available
            InstanceList instanceList = getInstanceList();
            for (Instance instance : instanceList.getItems()) {
                if (instance.getName().equals(instanceName)) {
                    //instance is available
                    //getInstance URL
                    instanceURL = instance.getSelfLink();
                    return instanceURL;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Get instance resource URL from given instance name
     *
     * @param instanceId
     * @return
     */
    public static String getInstanceURLFromId(String instanceId) {

        //todo:remove try catch
        String instanceURL;
        try {
            //check whether the given instance is available
            InstanceList instanceList = getInstanceList();
            for (Instance instance : instanceList.getItems()) {
                //todo: recheck following line
                String instanceIdInIaaS = ZONE_NAME + "/" + instance.getName();
                if (instanceIdInIaaS.equals(instanceId)) {
                    //instance is available
                    //getInstance URL
                    instanceURL = instance.getSelfLink();
                    return instanceURL;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }


    /**
     * This method is used for set port rage for forwarding rule
     */
    public void setPortRangeToForwardingRule() {
        //todo:implement this method
    }

    /**
     * Add a given list of instances to a target pool
     *
     * @param instancesIdsList - list of instance Ids
     * @param targetPoolName   - target pool name in IaaS
     */
    public void addInstancesToTargetPool(List<String> instancesIdsList, String targetPoolName) {

        log.info("Adding instances to target pool");

        if (instancesIdsList.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot add instances to target pool. InstancesNamesList is empty.");
            }
            return;
        }

        List<InstanceReference> instanceReferenceList = new ArrayList<InstanceReference>();

        //add instance to instance reference list, we should use the instance URL
        for (String instanceId : instancesIdsList) { //for all instances

            instanceReferenceList.add(new InstanceReference().
                    setInstance(getInstanceURLFromId(instanceId)));

        }

        //create target pools add instance request and set instance to it
        TargetPoolsAddInstanceRequest targetPoolsAddInstanceRequest = new
                TargetPoolsAddInstanceRequest();
        targetPoolsAddInstanceRequest.setInstances(instanceReferenceList);

        //todo Remove try catch
        try {
            //execute
            Operation operation = compute.targetPools().addInstance(PROJECT_ID, REGION_NAME,
                    targetPoolName, targetPoolsAddInstanceRequest).execute();

            waitForRegionOperationCompletion(operation.getName());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a health check in IaaS
     *
     * @param healthCheckName - name of the health check to be created
     */
    public void createHealthCheck(String healthCheckName) {

        if (!isHealthCheckExists(healthCheckName)) {//if the health check is not present already

            HttpHealthCheck httpHealthCheck = new HttpHealthCheck();
            httpHealthCheck.setName(healthCheckName);
            httpHealthCheck.setRequestPath(HEALTH_CHECK_REQUEST_PATH);
            //TODO: read as integers
            httpHealthCheck.setPort(Integer.parseInt(HEALTH_CHECK_PORT));
            httpHealthCheck.setTimeoutSec(Integer.parseInt(HEALTH_CHECK_TIME_OUT_SEC));
            httpHealthCheck.setUnhealthyThreshold(Integer.parseInt(HEALTH_CHECK_UNHEALTHY_THRESHOLD));
            try {
                Operation operation = compute.httpHealthChecks().insert(PROJECT_ID, httpHealthCheck).execute();
                waitForGlobalOperationCompletion(operation.getName());

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Health check is already exist: " + healthCheckName);
            }
        }
    }

    /**
     * Checking whether a given health check is exists or not
     *
     * @param healthCheckName - name of the health check
     * @return
     */
    public boolean isHealthCheckExists(String healthCheckName) {
        try {
            HttpHealthCheckList httpHealthCheckList = compute.httpHealthChecks().list(PROJECT_ID).execute();
            for (HttpHealthCheck httpHealthCheck : httpHealthCheckList.getItems()) {
                if (httpHealthCheck.getName().equals(healthCheckName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //todo: remove this method(never used)
    public HttpHealthCheck getHealthCheckFromName(String healthCheckName) {

        HttpHealthCheckList healthCheckList = getHealthCheckList();
        for (HttpHealthCheck httpHealthCheck : healthCheckList.getItems()) {
            if (httpHealthCheck.getName().equals(healthCheckName)) {
                //healthcheck is available
                return httpHealthCheck;
            }
        }
        return null;
    }

    /**
     * Get a given health check resource URL
     *
     * @param healthCheckName - name of the health check in IaaS
     * @return
     */
    public static String getHealthCheckURLFromName(String healthCheckName) {

        String healthCheckURL;

        //check whether the given instance is available
        HttpHealthCheckList healthCheckList = getHealthCheckList();
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
     * @return
     */
    public static HttpHealthCheckList getHealthCheckList() {
        try {
            Compute.HttpHealthChecks.List healthChecks = compute.httpHealthChecks().list(PROJECT_ID);
            HttpHealthCheckList healthCheckList = healthChecks.execute();
            if (healthCheckList.getItems() == null) {
                log.info("No health check found for specified project");
                return null;
            } else {
                return healthCheckList;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Wait for a global operation completion. If the operation is related to whole project
     * that is a global operation
     *
     * @param operationName - name of the operation in IaaS
     */
    private void waitForGlobalOperationCompletion(String operationName) {
        try {
            Thread.sleep(2000);
            while (true) {
                Operation operation = compute.globalOperations().get(PROJECT_ID, operationName).execute();
                if (operation.getStatus().equals("DONE")) {
                    return;
                }
                Thread.sleep(1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for a region operation completion. If the operation is related to a region that is a
     * region operation
     *
     * @param operationName - name of the region operation
     */

    private void waitForRegionOperationCompletion(String operationName) {
        try {
            Thread.sleep(2000);
            while (true) {
                Operation operation = compute.regionOperations().get(PROJECT_ID, REGION_NAME, operationName).execute();

                if (operation.getStatus().equals("DONE")) {
                    return;
                }
                Thread.sleep(1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * wait for zone operation completion. If the operation is related to a zone, that is a zone operation
     *
     * @param operationName -operation name
     */
    private void waitForZoneOperationCompletion(String operationName) {
        try {
            while (true) {
                Operation operation = compute.regionOperations().get(PROJECT_ID, REGION_NAME, operationName).execute();
                if (operation.getStatus().equals("DONE")) {
                    return;
                }
                Thread.sleep(1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //a test method
    public void addInstanceToTargetPool(String instanceId) {

        List<InstanceReference> instanceReferenceList = new ArrayList<InstanceReference>();

        //remove instance to instance reference list, we should use the instance URL
        InstanceReference instanceReference1 = new InstanceReference();
        instanceReference1.setInstance(getInstanceURLFromName(instanceId));
        instanceReferenceList.add(instanceReference1);
        addInstancesToTargetPool("testtargetpool", instanceReferenceList);

    }

    /**
     * this is a sample method using for testing purposes
     */
    public void sampleMethodForAddingInstancesToTargetPool() {
        List<InstanceReference> instanceReferenceList = new ArrayList<InstanceReference>();

        //remove instance to instance reference list, we should use the instance URL
        InstanceReference instanceReference1 = new InstanceReference();
        instanceReference1.setInstance(getInstanceURLFromName("instance-2"));
        instanceReferenceList.add(instanceReference1);
        addInstancesToTargetPool("testtargetpool", instanceReferenceList);
    }

    /**
     * this is a sample method using for testing purposes
     */
    public void sampleMethodForRemovingInstancesToTargetPool() {
        List<InstanceReference> instanceReferenceList = new ArrayList<InstanceReference>();

        //add instances to instance reference list, we should use the instance URL
        InstanceReference instanceReference1 = new InstanceReference();
        instanceReference1.setInstance(getInstanceURLFromName("instance-2"));
        instanceReferenceList.add(instanceReference1);
        removeInstancesFromTargetPool("testtargetpool", instanceReferenceList);
    }

}