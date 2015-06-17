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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

//TODO: exception handling
public class GCELoadBalancer implements LoadBalancer {

    private static final Log log = LogFactory.getLog(GCELoadBalancer.class);

    private GCEOperations gceOperations;
    /**
     * We have one configuration per cluster
     * One cluster  has one target pool , one forwarding rule and a health check
     * This hash map is used to hold cluster IDs and corresponding configuration
     * So one cluster will have one loadBalancerConfiguration object
     */
    private HashMap<String, GCELoadBalancerConfiguration> clusterToLoadBalancerConfigurationMap;

    //protocol should be TCP or UDP
    private String protocol = "TCP";

    public GCELoadBalancer() {
        try {
            gceOperations = new GCEOperations();
            clusterToLoadBalancerConfigurationMap = new HashMap<String, GCELoadBalancerConfiguration>();
        } catch (LoadBalancerExtensionException e) {
            log.error(e);
        } catch (GeneralSecurityException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Override
    public void start() throws LoadBalancerExtensionException {
        //topology has completed

        log.info("Starting GCE Load balancer instance");

        //iterate through clusterToLoadBalancerConfigurationMap
        //for each cluster{
        //if Load balancer status == not running -->
        //      execute commands through GCE API for start the load balancer
        //}

        Iterator iterator = clusterToLoadBalancerConfigurationMap.entrySet().iterator();
        while (iterator.hasNext()) { //for each Load balancer configuration

            Map.Entry clusterIDLoadBalancerConfigurationPair = (Map.Entry) iterator.next();

            GCELoadBalancerConfiguration GCELoadBalancerConfiguration =
                    ((GCELoadBalancerConfiguration) clusterIDLoadBalancerConfigurationPair.getValue());

            if (!GCELoadBalancerConfiguration.getStatus()) { //if the load balancer is NOT already running

                //create a health check
                gceOperations.createHealthCheck(GCELoadBalancerConfiguration.getHealthCheckName());

                gceOperations.createFirewallRule();

                //crate a target pool in GCE
                gceOperations.createTargetPool(GCELoadBalancerConfiguration.getTargetPoolName(),
                        GCELoadBalancerConfiguration.getHealthCheckName());

                //add instances to target pool
                gceOperations.addInstancesToTargetPool(GCELoadBalancerConfiguration.getInstancesList(),
                        GCELoadBalancerConfiguration.getTargetPoolName());

                //create forwarding rules in GCE
                List<Integer> ipList = GCELoadBalancerConfiguration.getIpList();
                //need to create a port range String
                String portRange = "";
                //if the ip list is empty
                if (ipList.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Ip list is null");
                    }
                    //as a temporary solution set all ports to be open
                    portRange = "1-65535";

                }
                //else if ip list has only one value
                else if (ipList.size() == 1) {
                    portRange = Integer.toString(ipList.get(0)) + "-" + Integer.toString(ipList.get(0));
                }
                //else we have more than 1 value
                else {
                    //first we need to take the port range. So arrange ipList in ascending order
                    Collections.sort(ipList);
                    //take the first one and last one
                    portRange = Integer.toString(ipList.get(0)) + "-" + Integer.toString(ipList.get(ipList.size() - 1));
                }

                //create the forwarding rule
                gceOperations.createForwardingRule(GCELoadBalancerConfiguration.getForwardingRuleName(),
                        GCELoadBalancerConfiguration.getTargetPoolName(), protocol, portRange);

                //set status to running
                GCELoadBalancerConfiguration.setStatus(true);
            }

        }

        log.info("GCE Load balancer instance started");

    }

    @Override
    public void stop() throws LoadBalancerExtensionException {

        log.info("GCE Load Balancer is stopping");
        //iterate through hashmap and remove all

        Iterator iterator = clusterToLoadBalancerConfigurationMap.entrySet().iterator();
        while (iterator.hasNext()) { //for each Load balancer configuration

            Map.Entry clusterIDLoadBalancerConfigurationPair = (Map.Entry) iterator.next();
            GCELoadBalancerConfiguration GCELoadBalancerConfiguration =
                    ((GCELoadBalancerConfiguration) clusterIDLoadBalancerConfigurationPair.getValue());

            if (GCELoadBalancerConfiguration.getStatus()) { //if the load balancer is  already running

                gceOperations.deleteForwardingRule(GCELoadBalancerConfiguration.getForwardingRuleName());
                //delete target pool from GCE
                gceOperations.deleteTargetPool(GCELoadBalancerConfiguration.getTargetPoolName());

            }
        }

    }

    /**
     * Listen to latest topology and update load balancer configuration
     *
     * @param topology latest topology to be configured
     * @return
     * @throws LoadBalancerExtensionException
     */
    @Override
    public boolean configure(Topology topology) throws LoadBalancerExtensionException {

        log.info("Configuring Load balancer ");

        for (Service service : topology.getServices()) {
            for (Cluster cluster : service.getClusters()) { //for each cluster
                //we create one load balancer per cluster
                //Only create configuration object. Not execute.

                //check whether this cluster has a forwarding rule configuration or not
                if (clusterToLoadBalancerConfigurationMap.containsKey(cluster.getClusterId())) {

                    //It has a loadBalancer configured. Take it and update it as the given topology.

                    //get load balancer configuration
                    GCELoadBalancerConfiguration GCELoadBalancerConfiguration = clusterToLoadBalancerConfigurationMap.
                            get(cluster.getClusterId());

                    //check and update
                    List<String> updatedInstancesList = new ArrayList<String>();
                    List<Integer> updatedIPList = new ArrayList<Integer>();


                    for (Member member : cluster.getMembers()) {

                      if(member.getInstanceId() != null) {
                          if (!updatedInstancesList.contains(member.getInstanceId())) {
                              updatedInstancesList.add(member.getInstanceId());
                          }
                      }

                        //checking for forwarding rules and updating
                        for (Object port : member.getPorts()) {
                            int portValue = ((Port) port).getValue();
                            //if not present update it
                            if (!updatedIPList.contains(portValue)) { //if port is not in list
                                //put the forwarding rule to list
                                updatedIPList.add(portValue);
                            }

                        }
                    }

                    //set new forwarding rules and instances list
                    GCELoadBalancerConfiguration.setIpList(updatedIPList);
                    GCELoadBalancerConfiguration.setInstancesList(updatedInstancesList);

                    if (GCELoadBalancerConfiguration.getTargetPoolName() == null) { //this does not have a target pool name
                        //set target pool name
                        String targetPoolName = targetPoolNameCreator(cluster.getClusterId());
                        GCELoadBalancerConfiguration.setTargetPoolName(targetPoolName);
                    }

                    if (GCELoadBalancerConfiguration.getForwardingRuleName() == null) {
                        //set forwarding rule name
                        String forwardingRuleName = forwardingRuleNameCreator(cluster.getClusterId());
                        GCELoadBalancerConfiguration.setForwardingRuleName(forwardingRuleName);
                    }

                    if (GCELoadBalancerConfiguration.getHealthCheckName() == null) {//if this does not have a forwarding rule name
                        //set health check name
                        String healthCheckName = healthCheckNameCreator(cluster.getClusterId());
                        GCELoadBalancerConfiguration.setHealthCheckName(healthCheckName);
                    }


                } else {
                    //doesn't have a GCELoadBalancerConfiguration object. So crate a new one and add to hash map

                    List<String> instancesList = new ArrayList<String>();
                    List<Integer> ipList = new ArrayList<Integer>();

                    for (Member member : cluster.getMembers()) {

                        //add instance to instance list
                        instancesList.add(member.getInstanceId());

                        //add forwarding rules(Ports to be forwarded)
                        for (Object port : member.getPorts()) {
                            int portValue = ((Port) port).getValue();
                            if (!ipList.contains(portValue)) { //if port is not in list
                                //put the forwarding rule to list
                                ipList.add(portValue);
                            }
                        }

                    }

                    GCELoadBalancerConfiguration GCELoadBalancerConfiguration = new GCELoadBalancerConfiguration(
                            cluster.getClusterId(), instancesList, ipList);

                    if (GCELoadBalancerConfiguration.getTargetPoolName() == null) { //this does not have a target pool name
                        //set target pool name
                        String targetPoolName = targetPoolNameCreator(cluster.getClusterId());
                        GCELoadBalancerConfiguration.setTargetPoolName(targetPoolName);
                    }

                    if (GCELoadBalancerConfiguration.getForwardingRuleName() == null) {
                        //set forwarding rule name
                        String forwardingRuleName = forwardingRuleNameCreator(cluster.getClusterId());
                        GCELoadBalancerConfiguration.setForwardingRuleName(forwardingRuleName);
                    }

                    if (GCELoadBalancerConfiguration.getHealthCheckName() == null) {//if this does not have a forwarding rule name
                        //set health check name
                        String healthCheckName = healthCheckNameCreator(cluster.getClusterId());
                        GCELoadBalancerConfiguration.setHealthCheckName(healthCheckName);
                    }

                    clusterToLoadBalancerConfigurationMap.put(cluster.getClusterId(), GCELoadBalancerConfiguration);

                }

            }
        }

        return true;
    }

    /**
     * @throws LoadBalancerExtensionException
     */
    @Override
    public void reload() throws LoadBalancerExtensionException {

        //iterate through hash map
        //find what needs to be changed
        //execute

    }

    /**
     * Create a valid target pool name
     *
     * @param clusterId - Id of the cluster
     * @return
     */
    private String targetPoolNameCreator(String clusterId) {
        //create a valid target pool name by using cluster ID
        //remove spaces, make all to lower case, replace all "." --> "-"
        //add name prefix
        String targetPoolName = GCEContext.getInstance().getNamePrefix().toLowerCase() + "-" +
                clusterId.trim().toLowerCase().replace(".", "-");
        //length should be les than 62 characters
        if (targetPoolName.length() >= 62) {
            targetPoolName = targetPoolName.substring(0, 62);
        }
        return targetPoolName;
    }

    /**
     * Create a valid forwarding rule name
     *
     * @param clusterID - Id of the cluster
     * @return
     */
    private String forwardingRuleNameCreator(String clusterID) {
        String forwardingRuleName = GCEContext.getInstance().getNamePrefix()
                + "-fr-" + clusterID.trim().toLowerCase().replace(".", "-");
        //length should be les than 62 characters
        if (forwardingRuleName.length() >= 62) {
            forwardingRuleName = forwardingRuleName.substring(0, 62);
        }
        return forwardingRuleName;
    }

    /**
     * create a valid health check name
     *
     * @param clusterID - id of the cluster
     * @return
     */
    private String healthCheckNameCreator(String clusterID) {
        String healthCheckName = GCEContext.getInstance().getNamePrefix().toLowerCase() + "-hc-" +
                clusterID.trim().toLowerCase().replace(".", "-");

        //length should be les than 62 characters
        if (healthCheckName.length() >= 62) {
            healthCheckName = healthCheckName.substring(0, 62);
        }

        return healthCheckName;

    }
}