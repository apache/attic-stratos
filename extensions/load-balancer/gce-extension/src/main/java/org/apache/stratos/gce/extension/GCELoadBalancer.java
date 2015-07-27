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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.ImmutableFieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.gce.extension.config.GCEClusterConfigurationHolder;
import org.apache.stratos.gce.extension.config.GCEContext;
import org.apache.stratos.gce.extension.util.GCEOperations;
import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

import java.util.*;

public class GCELoadBalancer implements LoadBalancer {

    private static final Log log = LogFactory.getLog(GCELoadBalancer.class);
    //PROTOCOL should be TCP or UDP
    private static final String PROTOCOL = "TCP";
    private GCEOperations gceOperations;
    /**
     * We have one configuration per cluster
     * One cluster  has one target pool , one forwarding rule and a health check
     * This hash map is used to hold cluster IDs and corresponding configuration
     * So one cluster will have one loadBalancerConfiguration object
     */
    private HashMap<String, GCEClusterConfigurationHolder> clusterToLoadBalancerConfigurationMap;

    public GCELoadBalancer() {
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
        log.info("Topology received. Configuring Load balancer ");

        //printing the topology for testing purposes
        XStream xstream = new XStream(new Sun14ReflectionProvider(
                new FieldDictionary(new ImmutableFieldKeySorter())),
                new DomDriver("utf-8"));
        log.info(xstream.toXML(topology));

        try {

            //this list is used to hold the current clusters available in topology and which has at least one member.
            List<String> activeClusterIdList = new ArrayList<String>();

            for (Service service : topology.getServices()) {
                for (Cluster cluster : service.getClusters()) { //for each cluster

                    //check whether this cluster has a load balancer configuration or not
                    if (clusterToLoadBalancerConfigurationMap.containsKey(cluster.getClusterId())) {

                        log.info("Reconfiguring the existing cluster: " + cluster.getClusterId());

                        //It already has a entry in clusterToLoadBalancerConfigurationMap.
                        //Take it and update it as the given topology.
                        GCEClusterConfigurationHolder gceClusterConfigurationHolder = clusterToLoadBalancerConfigurationMap.
                                get(cluster.getClusterId());

                        //if the cluster contains at least one member
                        if (cluster.getMembers().size() > 0) {
                            //that cluster contains at least one member

                            activeClusterIdList.add(cluster.getClusterId());


                            //***************detect member changes and update**************//

                            //check for newly created members
                            List<String> membersToBeAddedToTargetPool = new ArrayList<String>();
                            for (Member member : cluster.getMembers()) {

                                if (member.getInstanceId() != null) {
                                    if (!gceClusterConfigurationHolder.getMemberList().contains(member.getInstanceId())) {
                                        membersToBeAddedToTargetPool.add(member.getInstanceId());
                                    }
                                }

                            }

                            if (membersToBeAddedToTargetPool.size() > 0) { //we have new members

                                log.info("New members in cluster" + cluster.getClusterId() + " found. Adding new members " +
                                        "to cluster");

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
                                    membersToBeRemovedFromTargetPool.add(memberId);
                                }
                            }

                            if (membersToBeRemovedFromTargetPool.size() > 0) { //found terminated members
                                log.info("Terminated members found in cluster " + cluster.getClusterId() + ". Removing them");

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

                        if (cluster.getMembers().size() == 0) {
                            log.info("Cluster " + cluster.getClusterId() + " does not have any members. So not configuring");
                        } else {
                            activeClusterIdList.add(cluster.getClusterId());
                            List<String> instancesList = new ArrayList<String>();
                            List<Integer> ipList = new ArrayList<Integer>();
                            for (Member member : cluster.getMembers()) {

                                //add instance to instance list
                                if (member.getInstanceId() != null) {
                                    instancesList.add(member.getInstanceId());
                                }

                                //add forwarding rules(Ports to be forwarded)
                                for (Object port : member.getPorts()) {
                                    int portValue = ((Port) port).getValue();
                                    if (!ipList.contains(portValue)) { //if port is not in list
                                        //put the forwarding rule to list
                                        ipList.add(portValue);
                                    }
                                }

                            }

                            GCEClusterConfigurationHolder GCEClusterConfigurationHolder = new GCEClusterConfigurationHolder(
                                    cluster.getClusterId(), instancesList, ipList);

                            //set target pool name
                            String targetPoolName = targetPoolNameCreator(cluster.getClusterId());
                            GCEClusterConfigurationHolder.setTargetPoolName(targetPoolName);

                            //set forwarding rule name
                            String forwardingRuleName = forwardingRuleNameCreator(cluster.getClusterId());
                            GCEClusterConfigurationHolder.setForwardingRuleName(forwardingRuleName);

                            //set health check name
                            String healthCheckName = healthCheckNameCreator(cluster.getClusterId());
                            GCEClusterConfigurationHolder.setHealthCheckName(healthCheckName);

                            clusterToLoadBalancerConfigurationMap.put(cluster.getClusterId(), GCEClusterConfigurationHolder);
                            createConfigurationForCluster(cluster.getClusterId());

                        }
                    }

                }
            }

            //if any cluster is removed from the topology or if any cluster does not have at least one member,
            //remove those clusters from map and remove the configuration from GCE too
            for (String clusterId : clusterToLoadBalancerConfigurationMap.keySet()) {
                if (!activeClusterIdList.contains(clusterId)) {
                    log.info("Removing the configuration for cluster " + clusterId);
                    clusterToLoadBalancerConfigurationMap.remove(clusterId);
                    deleteConfigurationForCluster(clusterId);
                }
            }

            log.info("Load balancer configured as given topology");
            return true;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to configure load balancer");
            }
            throw new LoadBalancerExtensionException(e);
        }
    }

    /**
     * This method is used to delete all fowarding rules, target pools and health checks
     * in IaaS side according to new topology
     */
    private void deleteConfigurationForCluster(String clusterId) throws LoadBalancerExtensionException {

        try {
            log.info("Deleting forwarding rule for cluster " + clusterId);
            GCEClusterConfigurationHolder gceClusterConfigurationHolder = clusterToLoadBalancerConfigurationMap.get(clusterId);
            //delete forwarding rule
            gceOperations.deleteForwardingRule(gceClusterConfigurationHolder.getForwardingRuleName());
            //delete target pool from GCE
            gceOperations.deleteTargetPool(gceClusterConfigurationHolder.getTargetPoolName());
            //delete health check from GCE
            gceOperations.deleteHealthCheck(gceClusterConfigurationHolder.getHealthCheckName());
            log.info("Deleted forwarding rule for cluster " + clusterId);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not delete configuration for cluster " + clusterId);
            }
            throw new LoadBalancerExtensionException(e);
        }

    }

    /**
     * This method is used to create target pools, forwarding rule and a health check related to a cluster
     * in IaaS side according to new topology
     */
    private void createConfigurationForCluster(String clusterId) throws LoadBalancerExtensionException {

        try {

            log.info("Creating configuration for cluster");

            GCEClusterConfigurationHolder gceClusterConfigurationHolder = clusterToLoadBalancerConfigurationMap.get(clusterId);

            //create a health check
            gceOperations.createHealthCheck(gceClusterConfigurationHolder.getHealthCheckName());

            gceOperations.createFirewallRule();

            //crate a target pool in GCE
            gceOperations.createTargetPool(gceClusterConfigurationHolder.getTargetPoolName(),
                    gceClusterConfigurationHolder.getHealthCheckName());

            //add instances to target pool
            gceOperations.addInstancesToTargetPool(gceClusterConfigurationHolder.getMemberList(),
                    gceClusterConfigurationHolder.getTargetPoolName());

            //create forwarding rules in GCE
            List<Integer> ipList = gceClusterConfigurationHolder.getIpList();
            //need to create a port range String
            String portRange;
            //if the ip list is empty
            if (ipList.isEmpty()) {
                log.warn("Ip list is null");
                //set all ports to be opened
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
            gceOperations.createForwardingRule(gceClusterConfigurationHolder.getForwardingRuleName(),
                    gceClusterConfigurationHolder.getTargetPoolName(), PROTOCOL, portRange);
            log.info("Created configuration for cluster");
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create configuration for cluster " + clusterId);
            }
            log.error(e.getMessage());
            throw new LoadBalancerExtensionException(e);
        }
    }

    @Override
    public void start() throws LoadBalancerExtensionException {
        //Configuration has completed
        log.info("GCE Load balancer instance started");

    }

    @Override
    public void stop() throws LoadBalancerExtensionException {

        log.info("GCE Load Balancer is stopping");

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
        log.info("Configuration reloaded");

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
        if (targetPoolName.length() >= 62) {
            targetPoolName = targetPoolName.substring(0, 62);
        }
        return targetPoolName;
    }

    /**
     * Create a valid forwarding rule name
     *
     * @param clusterID - Id of the cluster
     * @return - a proper name for forwarding rule
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
     * @return - a proper name for health check
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