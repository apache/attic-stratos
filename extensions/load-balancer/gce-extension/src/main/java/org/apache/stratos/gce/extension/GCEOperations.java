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

import com.google.api.services.compute.model.Operation.Error.Errors;

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
    private static final String HEALTH_CHECK_TIME_OUT_SEC =GCEContext.getInstance().getHealthCheckTimeOutSec();
    private static final String HEALTH_CHECK_UNHEALTHY_THRESHOLD = GCEContext.getInstance().getHealthCheckUnhealthyThreshold();

    /**
     * Directory to store user credentials.
     */
    private static final String DATA_STORE_DIR = ".store/gce-extension";

    private static final String RUNNING_FILTER = "status eq RUNNING";


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
     * @param targetPoolName
     */
    public void createTargetPool(String targetPoolName) {

        TargetPool targetPool = new TargetPool();
        targetPool.setName(targetPoolName);

        //TODO:REMOVE try catch
        try {
            Operation operation =
                    compute.targetPools().insert(PROJECT_ID, REGION_NAME, targetPool).execute();
            //wait until operation succeed
            while (true){
                //todo: correct this line
                if(operation.getStatus().equals("DONE")){

                    //log response
                    if(log.isDebugEnabled()){
                        log.debug("Target pool creation operation Status: "+operation.getStatusMessage());
                    }
                    if (log.isErrorEnabled()) {
                        for ( Errors errors : operation.getError().getErrors()) {
                            log.error("Target pool creation operation error: " + errors.getMessage());
                        }
                    }
                    return;
                }
                if(log.isDebugEnabled()){
                    log.debug("Waiting until the create target pool API call get succeeded");
                }
                Thread.sleep(100);
            }

        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("failed to create target pool: " + targetPoolName);
            }
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    public void deleteTargetPool(String targetPoolName){
        try {
            Operation operation = compute.targetPools().delete(PROJECT_ID, REGION_NAME, targetPoolName).execute();
            //wait until operation succeed
            while (true){                //todo: correct this line

                if(operation.getStatus().equals("DONE")){

                    //log response
                    if(log.isDebugEnabled()){
                        log.debug("Delete target pool operation Status: "+operation.getStatusMessage());
                    }
                    if (log.isErrorEnabled()) {
                        for ( Errors errors : operation.getError().getErrors()) {
                            log.error("Delete target pool operation error: " + errors.getMessage());
                        }
                    }
                    return;
                }
                if(log.isDebugEnabled()){
                    log.debug("Waiting until the delete target pool API call get succeeded");
                }
                Thread.sleep(100);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * create forwarding rule by using given target pool and protocol
     *
     * @param forwardingRuleName
     * @param targetPoolName
     * @param protocol
     */

    public void createForwardingRule(String forwardingRuleName, String targetPoolName, String protocol) {

        //Need to get target pool resource URL
        TargetPool targetPool = getTargetPool(targetPoolName);
        String targetPoolURL = targetPool.getSelfLink();
        ForwardingRule forwardingRule = new ForwardingRule();
        forwardingRule.setName(forwardingRuleName);
        forwardingRule.setTarget(targetPoolURL);
        forwardingRule.setIPProtocol(protocol);
        try {
            Operation operation = compute.forwardingRules().insert(PROJECT_ID, REGION_NAME, forwardingRule).execute();

            //wait until operation succeed
            while (true){                //todo: correct this line

                if(operation.getStatus().equals("DONE")){

                    //log response
                    if(log.isDebugEnabled()){
                        log.debug("Create forwarding rule operation Status: "+operation.getStatusMessage());
                    }
                    if (log.isErrorEnabled()) {
                        for ( Errors errors : operation.getError().getErrors()) {
                            log.error("Create forwarding rule operation error: " + errors.getMessage());
                        }
                    }
                    return;
                }
                if(log.isDebugEnabled()){
                    log.debug("Waiting until the create forwarding rule API call get succeeded");
                }
                Thread.sleep(100);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void deleteForwardingRule(String forwardingRuleName){
        try {
            Operation operation = compute.forwardingRules().
                    delete(PROJECT_ID, REGION_NAME, forwardingRuleName).execute();
            //wait until operation succeed
            while (true){
                if(operation.getStatus().equals("DONE")){                //todo: correct this line


                    //log response
                    if(log.isDebugEnabled()){
                        log.debug("delete forwarding rule operation Status: "+operation.getStatusMessage());
                    }
                    if (log.isErrorEnabled()) {
                        for ( Errors errors : operation.getError().getErrors()) {
                            log.error("delete forwarding rule operation error: " + errors.getMessage());
                        }
                    }
                    return;
                }
                if(log.isDebugEnabled()){
                    log.debug("Waiting until the delete forwarding rule API call get succeeded");
                }
                Thread.sleep(100);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Check whether the given target pool is already exists in the given project and region.
     * Target pools are unique for regions, not for zones
     *
     * @param targetPoolName
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
     * @param targetPoolName
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
     * @param targetPoolName
     * @param instanceReferenceList
     */
    private void addInstancesToTargetPool(String targetPoolName, List<InstanceReference>
            instanceReferenceList) {



    }

    /**
     * Remove given set of instances from target pool
     *
     * @param targetPoolName
     * @param instanceReferenceList
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
        System.out.println("Listing running Compute Engine Instances");
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

    /**
     * Get instance resource URL from given instance name
     *
     * @param instanceName
     * @return
     */
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
     * This method is used for set port rage for forwarding rule
     */
    public void setPortRangeToForwardingRule() {
        //todo:implement this method
    }

    public void addInstancesToTargetPool(List<String> instancesNamesList, String targetPoolName){

        log.info("Adding instances to target pool");

        List<InstanceReference> instanceReferenceList = new ArrayList<InstanceReference>();

        //add instance to instance reference list, we should use the instance URL
        for(String instanceName : instancesNamesList ){ //for all instances

            instanceReferenceList.add(new InstanceReference().
                    setInstance(getInstanceURLFromName(instanceName)));

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
            //wait until operation succeed
            while (true){
                if(operation.getStatus().equals("DONE")){

                    //log response
                    if(log.isDebugEnabled()){
                        log.debug("Add instances to target pool operation Status: "+operation.getStatusMessage());
                    }
                    if (log.isErrorEnabled()) {
                        for ( Errors errors : operation.getError().getErrors()) {
                            log.error("Add instances to target pool operation error: " + errors.getMessage());
                        }
                    }
                    return;
                }
                if(log.isDebugEnabled()){
                    log.debug("Waiting until the Add instances to target pool API call get succeeded");
                }
                Thread.sleep(100);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createHealthCheck(String healthCheckName){

        if(!isHealthCheckExists(healthCheckName)){//if the health check is not present already

            HttpHealthCheck httpHealthCheck = new HttpHealthCheck();
            httpHealthCheck.setName(healthCheckName);
            httpHealthCheck.setRequestPath(HEALTH_CHECK_REQUEST_PATH);
            //TODO: read as integers
            httpHealthCheck.setPort(Integer.parseInt(HEALTH_CHECK_PORT));
            httpHealthCheck.setTimeoutSec(Integer.parseInt(HEALTH_CHECK_TIME_OUT_SEC));
            try {
                Operation operation = compute.httpHealthChecks().insert(PROJECT_ID,httpHealthCheck).execute();

                //wait until succeed
                while (true){
                    if(operation.getStatus().equals("DONE")){

                        //log response
                        if(log.isDebugEnabled()){
                            log.debug("Create health check operation Status: "+operation.getStatusMessage());
                        }
                        if (log.isErrorEnabled()) {
                            for ( Errors errors : operation.getError().getErrors()) {
                                log.error("Create create health check operation error: " + errors.getMessage());
                            }
                        }
                        return;
                    }
                    if(log.isDebugEnabled()){
                        log.debug("Waiting until the create health check API call get succeeded");
                    }
                    Thread.sleep(100);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        else {
            if(log.isDebugEnabled()) {
                log.debug("Health check is already exist: " + healthCheckName);
            }
        }
    }

    public boolean isHealthCheckExists(String healthCheckName){
        try {
            HttpHealthCheckList httpHealthCheckList =  compute.httpHealthChecks().list(PROJECT_ID).execute();
            for(HttpHealthCheck httpHealthCheck: httpHealthCheckList.getItems()){
                if(httpHealthCheck.getName().equals(healthCheckName)){
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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