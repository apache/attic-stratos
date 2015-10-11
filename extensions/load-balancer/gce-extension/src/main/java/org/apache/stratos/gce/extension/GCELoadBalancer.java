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
import org.apache.stratos.gce.extension.config.Constants;
import org.apache.stratos.gce.extension.config.GCEClusterConfigurationHolder;
import org.apache.stratos.gce.extension.config.GCEContext;
import org.apache.stratos.gce.extension.util.GCEOperations;
import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * All the methods in Stratos load balancer API have been implemented in this class
 */
public class GCELoadBalancer implements LoadBalancer {

    private static final Log log = LogFactory.getLog(GCELoadBalancer.class);
    private GCEOperations gceOperations;
    /**
     * One configuration object per cluster will be created
     * One cluster  has one target pool,one forwarding rule and a health check
     * This hash map is used to hold cluster IDs and corresponding configuration
     */
    private Map<String, GCEClusterConfigurationHolder> clusterToLoadBalancerConfigurationMap;

    public GCELoadBalancer() throws IOException, GeneralSecurityException {
        gceOperations = new GCEOperations();
        clusterToLoadBalancerConfigurationMap = new HashMap<String, GCEClusterConfigurationHolder>();
    }

    /**
     * Listen to latest topology and update load balancer configuration
     *
     * @param topology latest topology to be configured
     * @return - true - if the load balancer was successfully configured. else false
     * @throws LoadBalancerExtensionException
     */
    @Override
    public boolean configure(Topology topology) throws LoadBalancerExtensionException {
        log.info("Complete topology received. Configuring Load balancer...");

        //this list is used to hold the current clusters available in topology and which has at least one member.
        List<String> activeClusterIdList = new ArrayList<String>();

        for (Service service : topology.getServices()) {
            for (Cluster cluster : service.getClusters()) { //for each cluster

                //check whether this cluster has a load balancer configuration or not
                if (clusterToLoadBalancerConfigurationMap.containsKey(cluster.getClusterId())) {

                    if (log.isDebugEnabled()) {
                        log.debug("Reconfiguring the existing cluster: " + cluster.getClusterId() + "...");
                    }

                    //It already has a entry in clusterToLoadBalancerConfigurationMap.
                    //Take it and update it as the given topology.
                    GCEClusterConfigurationHolder gceClusterConfigurationHolder = clusterToLoadBalancerConfigurationMap.
                            get(cluster.getClusterId());

                    //if the cluster contains at least one member
                    if (!cluster.getMembers().isEmpty()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Cluster " + cluster.getClusterId() + " has one or more members");
                        }
                        activeClusterIdList.add(cluster.getClusterId());
                        if (log.isDebugEnabled()) {
                            log.debug("Cluster " + cluster.getClusterId() + " was added to active cluster id list");
                        }

                        //detect member changes and update
                        //check for newly created members
                        List<String> membersToBeAddedToTargetPool = new ArrayList<String>();
                        for (Member member : cluster.getMembers()) {

                            if (member.getInstanceId() != null && !gceClusterConfigurationHolder.getMemberList().
                                    contains(member.getInstanceId())) {
                                if (log.isDebugEnabled()) {
                                    log.debug("New member found: " + member.getInstanceId());
                                }
                                membersToBeAddedToTargetPool.add(member.getInstanceId());
                            }
                        }

                        if (!membersToBeAddedToTargetPool.isEmpty()) { //we have new members
                            log.info("New members in cluster" + cluster.getClusterId() + " found. Adding new members " +
                                    "to cluster...");

                            //add them to configuration holder
                            for (String memberId : membersToBeAddedToTargetPool) {
                                gceClusterConfigurationHolder.addMember(memberId);

                            }

                            //add them to target pool too
                            gceOperations.addInstancesToTargetPool(membersToBeAddedToTargetPool,
                                    gceClusterConfigurationHolder.getTargetPoolName());
                        }

                        //check for terminated members and remove them from cluster
                        List<String> membersToBeRemovedFromTargetPool = new ArrayList<String>();
                        for (String memberId : gceClusterConfigurationHolder.getMemberList()) { //for all members in Map
                            boolean found = false;
                            for (Member member : cluster.getMembers()) { //for all members in cluster
                                if (member.getInstanceId().equals(memberId)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                //add member id to membersToBeRemovedFromTargetPool in order remove member from map
                                if (log.isDebugEnabled()) {
                                    log.debug("Terminated member found: " + memberId);
                                }
                                membersToBeRemovedFromTargetPool.add(memberId);
                            }
                        }

                        if (!membersToBeRemovedFromTargetPool.isEmpty()) { //found terminated members
                            log.info("Terminated members found in cluster " + cluster.getClusterId() + ". Removing them...");

                            //remove them from configuration holder
                            for (String memberId : membersToBeRemovedFromTargetPool) {
                                gceClusterConfigurationHolder.removeMember(memberId);

                            }

                            //remove them from GCE too
                            gceOperations.removeInstancesFromTargetPool(membersToBeRemovedFromTargetPool,
                                    gceClusterConfigurationHolder.getTargetPoolName());
                        }
                    }
                } else {
                    //doesn't have a GCEClusterConfigurationHolder object. So crate a new one and add to hash map
                    log.info("Found a new cluster: " + cluster.getClusterId());

                    if (cluster.getMembers().isEmpty()) {
                        log.info("Cluster " + cluster.getClusterId() + " does not have any members. So not configuring");
                    } else {
                        activeClusterIdList.add(cluster.getClusterId());
                        List<String> instancesList = new ArrayList<String>();
                        List<Integer> ipList = new ArrayList<Integer>();
                        for (Member member : cluster.getMembers()) {

                            //add instance to instance list
                            if (member.getInstanceId() != null) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Adding member " + member.getMemberId() + " to instance list since the" +
                                            "member id is not null");
                                }
                                instancesList.add(member.getInstanceId());
                            } else {
                                if (log.isDebugEnabled()) {
                                    log.debug("Instance ID is null for " + member.getMemberId() + " so not adding to" +
                                            "the instance list");
                                }
                            }

                            //add forwarding rules(Ports to be forwarded)
                            for (Object port : member.getPorts()) {
                                int portValue = ((Port) port).getValue();
                                if (!ipList.contains(portValue)) { //if port is not in list
                                    //put the forwarding rule to list
                                    if (log.isDebugEnabled()) {
                                        log.debug("Port found: " + portValue);
                                    }
                                    ipList.add(portValue);
                                }
                            }
                        }

                        GCEClusterConfigurationHolder gceClusterConfigurationHolder = new GCEClusterConfigurationHolder(
                                cluster.getClusterId(), instancesList, ipList);

                        //set target pool name
                        String targetPoolName = targetPoolNameCreator(cluster.getClusterId());
                        gceClusterConfigurationHolder.setTargetPoolName(targetPoolName);

                        //set forwarding rule name
                        String forwardingRuleName = forwardingRuleNameCreator(cluster.getClusterId());
                        gceClusterConfigurationHolder.setForwardingRuleName(forwardingRuleName);

                        //set health check names
                        if (!ipList.isEmpty()) {
                            Collections.sort(ipList);
                            for (int port : ipList) {
                                String healthCheckName = healthCheckNameCreator(cluster.getClusterId(), port);
                                gceClusterConfigurationHolder.addHealthCheck(port, healthCheckName);
                            }
                        } else {
                            //the ip list is empty. So creating the default health check name
                            String healthCheckName = healthCheckNameCreator(cluster.getClusterId(),
                                    Integer.parseInt(GCEContext.getInstance().getHealthCheckPort()));
                            gceClusterConfigurationHolder.addHealthCheck(Integer.parseInt(GCEContext.
                                    getInstance().getHealthCheckPort()), healthCheckName);
                        }

                        clusterToLoadBalancerConfigurationMap.put(cluster.getClusterId(), gceClusterConfigurationHolder);
                        createConfigurationForCluster(cluster.getClusterId());
                    }
                }
            }
        }

        //if any cluster is removed from the topology or if any cluster does not have at least one member,
        //remove those clusters from map and remove the configuration from GCE too
        List<String> clustersToBeRemoved = new ArrayList<String>();
        for (String clusterId : clusterToLoadBalancerConfigurationMap.keySet()) {
            if (!activeClusterIdList.contains(clusterId)) {
                log.info("Removing the configuration for cluster " + clusterId + "...");
                clustersToBeRemoved.add(clusterId);
            }
        }
        for (String clusterId : clustersToBeRemoved) {
            deleteConfigurationForCluster(clusterId); //remove from GCE
            clusterToLoadBalancerConfigurationMap.remove(clusterId); //remove from local map
        }
        activeClusterIdList.clear();
        log.info("Load balancer was configured as given topology");
        return true;
    }

    /**
     * This method is used to delete all forwarding rules, target pools and health checks
     * in IaaS side according to new topology
     */
    private void deleteConfigurationForCluster(String clusterId) throws LoadBalancerExtensionException {

        log.info("Deleting configuration for cluster " + clusterId + "...");
        GCEClusterConfigurationHolder gceClusterConfigurationHolder = clusterToLoadBalancerConfigurationMap.get(clusterId);
        //delete forwarding rule
        gceOperations.deleteForwardingRule(gceClusterConfigurationHolder.getForwardingRuleName());
        //delete target pool from GCE
        gceOperations.deleteTargetPool(gceClusterConfigurationHolder.getTargetPoolName());

        //delete health checks from GCE
        Collection<String> healthCheckNames = gceClusterConfigurationHolder.getHealthCheckNames();
        for (String healthCheckName : healthCheckNames) {
            gceOperations.deleteHealthCheck(healthCheckName);
        }
        log.info("Deleted configuration for cluster " + clusterId);
    }

    /**
     * This method is used to create target pools, forwarding rule and a health check related to a cluster
     * in IaaS side according to new topology
     */
    private void createConfigurationForCluster(String clusterId) throws LoadBalancerExtensionException {

        log.info("Creating configuration for cluster " + clusterId + "...");

        GCEClusterConfigurationHolder gceClusterConfigurationHolder = clusterToLoadBalancerConfigurationMap.get(clusterId);

        List<Integer> ipList = gceClusterConfigurationHolder.getIpList();

        //create a port range String
        String portRange;

        //if the ip list is empty
        if (ipList.isEmpty()) {
            log.warn("Ip list is null");
            //set all default port range
            portRange = Constants.DEFAULT_PORT_RANGE;
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

        if (log.isDebugEnabled()) {
            log.debug("Port range set as: " + portRange);
        }

        //create all health checks
        Map healthCheckMap = gceClusterConfigurationHolder.getHealthCheckMap();
        Iterator iterator = healthCheckMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry portMapNamePair = (Map.Entry) iterator.next();
            gceOperations.createHealthCheck((Integer) portMapNamePair.getKey(), (String) portMapNamePair.getValue());
        }

        //crate a target pool in GCE
        gceOperations.createTargetPool(gceClusterConfigurationHolder.getTargetPoolName(),
                gceClusterConfigurationHolder.getHealthCheckNames());

        //add instances to target pool
        gceOperations.addInstancesToTargetPool(gceClusterConfigurationHolder.getMemberList(),
                gceClusterConfigurationHolder.getTargetPoolName());

        //create the forwarding rule
        gceOperations.createForwardingRule(gceClusterConfigurationHolder.getForwardingRuleName(),
                gceClusterConfigurationHolder.getTargetPoolName(), Constants.PROTOCOL, portRange);
        log.info("Created configuration for cluster" + clusterId);
    }

    @Override
    public void start() throws LoadBalancerExtensionException {
        //Configuration has completed
        log.info("GCE Load balancer instance started");
    }

    @Override
    public void stop() throws LoadBalancerExtensionException {

        log.info("GCE Load Balancer is stopping...");

        //iterate through hash map and remove all
        Iterator iterator = clusterToLoadBalancerConfigurationMap.entrySet().iterator();
        while (iterator.hasNext()) { //for each configuration
            Map.Entry clusterIDLoadBalancerConfigurationPair = (Map.Entry) iterator.next();
            deleteConfigurationForCluster((String) clusterIDLoadBalancerConfigurationPair.getKey());
        }

        log.info("GCE Load balancer stopped");
    }

    /**
     * @throws LoadBalancerExtensionException
     */
    @Override
    public void reload() throws LoadBalancerExtensionException {
        //nothing to do here
    }

    /**
     * Create a valid target pool name
     *
     * @param clusterId - Id of the cluster
     * @return - a proper name for target pool
     */
    private String targetPoolNameCreator(String clusterId) {
        //create a valid target pool name by using cluster ID
        //remove spaces, make all to lower case, replace all "." --> "-"
        //add name prefix
        String targetPoolName = GCEContext.getInstance().getNamePrefix().toLowerCase() + "-" +
                clusterId.trim().toLowerCase().replace(".", "-");
        //length should be les than 62 characters
        if (targetPoolName.length() >= Constants.MAX_NAME_LENGTH) {
            targetPoolName = targetPoolName.substring(0, Constants.MAX_NAME_LENGTH);
        }
        return targetPoolName;
    }

    /**
     * Create a valid forwarding rule name
     *
     * @param clusterId - Id of the cluster
     * @return - a proper name for forwarding rule
     */
    private String forwardingRuleNameCreator(String clusterId) {
        String forwardingRuleName = GCEContext.getInstance().getNamePrefix().toLowerCase() + "-" +
                clusterId.trim().toLowerCase().replace(".", "-");

        //length should be les than 62 characters
        if (forwardingRuleName.length() >= Constants.MAX_NAME_LENGTH) {
            forwardingRuleName = forwardingRuleName.substring(0, Constants.MAX_NAME_LENGTH);
        }
        return forwardingRuleName;
    }

    /**
     * create a valid health check name
     *
     * @param clusterId - id of the cluster
     * @return - a proper name for health check
     */
    private String healthCheckNameCreator(String clusterId, int port) {
        String healthCheckName = GCEContext.getInstance().getNamePrefix().toLowerCase() + "-" +
                clusterId.trim().toLowerCase().replace(".", "-");

        //length should be less than 62 characters
        //keep 6 characters to add the port at the end
        if (healthCheckName.length() >= Constants.MAX_NAME_LENGTH) {
            healthCheckName = healthCheckName.substring(0, Constants.MAX_NAME_LENGTH - 6);
        }

        //add the port number at the end
        healthCheckName.concat("-" + Integer.toString(port));
        return healthCheckName;
    }
}