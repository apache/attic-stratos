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
import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

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

        gceOperations = new GCEOperations();
        clusterToLoadBalancerConfigurationMap = new HashMap<String, GCELoadBalancerConfiguration>();

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

        log.info("Topology received. Configuring Load balancer ");

        //printing the topology for tesging purposes
        XStream xstream = new XStream(new Sun14ReflectionProvider(
                new FieldDictionary(new ImmutableFieldKeySorter())),
                new DomDriver("utf-8"));
        log.info(xstream.toXML(topology));
        try {
            //check whether any cluster is removed. If removed, then remove the cluster from
            //clusterToLoadBalancerConfigurationMap and remove all objects in IaaS side too

            Iterator iterator = clusterToLoadBalancerConfigurationMap.entrySet().iterator();
            while (iterator.hasNext()) { //for each configuration

                Map.Entry clusterIDLoadBalancerConfigurationPair = (Map.Entry) iterator.next();
                GCELoadBalancerConfiguration gceLoadBalancerConfiguration =
                        ((GCELoadBalancerConfiguration) clusterIDLoadBalancerConfigurationPair.getValue());

                boolean found = false;

                //check whether cluster is in the map or not
                for (Service service : topology.getServices()) {
                    for (Cluster cluster : service.getClusters()) { //for each cluster
                        if (cluster.getClusterId().equals(gceLoadBalancerConfiguration.getClusterID())) {
                            found = true;
                            break;
                        }
                    }
                    if (found == true) {
                        break;
                    }
                }
                if (found == false) {
                    //remove cluster from map
                    log.info("Removed cluster is found. Remove it from GCE too. Cluster Id: " +
                            gceLoadBalancerConfiguration.getClusterID());
                    clusterToLoadBalancerConfigurationMap.remove(gceLoadBalancerConfiguration.getClusterID());
                    deleteConfigurationForCluster(gceLoadBalancerConfiguration.getClusterID());
                }
            }


            for (Service service : topology.getServices()) {
                for (Cluster cluster : service.getClusters()) { //for each cluster

                    //check whether this cluster has a forwarding rule configuration or not
                    if (clusterToLoadBalancerConfigurationMap.containsKey(cluster.getClusterId())) {

                        log.info("Reconfiguring the existing cluster: " + cluster.getClusterId());

                        //It already has a entry in clusterToLoadBalancerConfigurationMap.
                        //Take it and update it as the given topology.

                        GCELoadBalancerConfiguration gceLoadBalancerConfiguration = clusterToLoadBalancerConfigurationMap.
                                get(cluster.getClusterId());


                        //if the cluster does not contain at least one member
                        if (cluster.getMembers().size() == 0) {

                            log.info("Cluster: " + cluster.getClusterId() + " does not have any member.So remove cluster " +
                                    "from GCE too");
                            //remove all
                            deleteConfigurationForCluster(cluster.getClusterId());
                            clusterToLoadBalancerConfigurationMap.remove(gceLoadBalancerConfiguration.getClusterID());

                        } else {
                            //that cluster contains at least one member

                            //***************detect member changes and update**************//

                            //check for newly created members
                            List<String> membersToBeAddedToTargetPool = new ArrayList<String>();
                            for (Member member : cluster.getMembers()) {

                                if (member.getInstanceId() != null) {
                                    if (!gceLoadBalancerConfiguration.getMemberList().contains(member.getInstanceId())) {
                                        membersToBeAddedToTargetPool.add(member.getInstanceId());
                                        gceLoadBalancerConfiguration.addMember(member.getInstanceId());
                                    }
                                }

                            }

                            if (membersToBeAddedToTargetPool.size() > 0) { //we have new members

                                log.info("New members in cluster" + cluster.getClusterId() + " found. Adding new members " +
                                        "to cluster");
                                //add to target pool
                                gceOperations.addInstancesToTargetPool(membersToBeAddedToTargetPool,
                                        gceLoadBalancerConfiguration.getTargetPoolName());
                            }

                            //check for terminated members and remove them from cluster
                            List<String> membersToBeRemovedFromTargetPool = new ArrayList<String>();
                            for (String memberId : gceLoadBalancerConfiguration.getMemberList()) { //for all members in Map
                                boolean found = false;
                                for (Member member : cluster.getMembers()) { //for all members in cluster
                                    //todo: retest this line
                                    if (member.getInstanceId().equals(memberId)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found == false) {
                                    //remove member from map
                                    gceLoadBalancerConfiguration.removeMember(memberId);
                                    membersToBeRemovedFromTargetPool.add(memberId);
                                }
                            }

                            if (membersToBeRemovedFromTargetPool.size() > 0) { //found terminated members

                                log.info("Terminated members found in cluster " + cluster.getClusterId() + ". Removing them");

                                //remove them
                                gceOperations.removeInstancesFromTargetPool(membersToBeRemovedFromTargetPool,
                                        gceLoadBalancerConfiguration.getTargetPoolName());
                            }

                            //****************detect port map changes and update it*************//

                            //todo detect port map changes and update it

                        /*   //checking for forwarding rules and updating
                        for (Object port : member.getPorts()) {
                            int portValue = ((Port) port).getValue();
                            //if not present update it
                            if (!updatedIPList.contains(portValue)) { //if port is not in list
                                //put the forwarding rule to list
                                updatedIPList.add(portValue);
                            }

                        }
                        */

                            //********************null checks**************//

                            if (gceLoadBalancerConfiguration.getTargetPoolName() == null) { //this does not have a target pool name
                                //set target pool name
                                String targetPoolName = targetPoolNameCreator(cluster.getClusterId());
                                gceLoadBalancerConfiguration.setTargetPoolName(targetPoolName);
                            }

                            if (gceLoadBalancerConfiguration.getForwardingRuleName() == null) {
                                //set forwarding rule name
                                String forwardingRuleName = forwardingRuleNameCreator(cluster.getClusterId());
                                gceLoadBalancerConfiguration.setForwardingRuleName(forwardingRuleName);
                            }

                            if (gceLoadBalancerConfiguration.getHealthCheckName() == null) {//if this does not have a forwarding rule name
                                //set health check name
                                String healthCheckName = healthCheckNameCreator(cluster.getClusterId());
                                gceLoadBalancerConfiguration.setHealthCheckName(healthCheckName);
                            }

                        }

                    } else {
                        //doesn't have a GCELoadBalancerConfiguration object. So crate a new one and add to hash map

                        log.info("Found a new cluster: " + cluster.getClusterId());

                        if (cluster.getMembers().size() == 0) {
                            log.info("Cluster " + cluster.getClusterId() + "does not have any members. So not configuring");
                        } else {
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

                            GCELoadBalancerConfiguration GCELoadBalancerConfiguration = new GCELoadBalancerConfiguration(
                                    cluster.getClusterId(), instancesList, ipList);

                            //set target pool name
                            String targetPoolName = targetPoolNameCreator(cluster.getClusterId());
                            GCELoadBalancerConfiguration.setTargetPoolName(targetPoolName);

                            //set forwarding rule name
                            String forwardingRuleName = forwardingRuleNameCreator(cluster.getClusterId());
                            GCELoadBalancerConfiguration.setForwardingRuleName(forwardingRuleName);

                            //set health check name
                            String healthCheckName = healthCheckNameCreator(cluster.getClusterId());
                            GCELoadBalancerConfiguration.setHealthCheckName(healthCheckName);


                            clusterToLoadBalancerConfigurationMap.put(cluster.getClusterId(), GCELoadBalancerConfiguration);
                            createConfigurationForCluster(cluster.getClusterId());

                        }
                    }

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
            GCELoadBalancerConfiguration gceLoadBalancerConfiguration = clusterToLoadBalancerConfigurationMap.get(clusterId);
            //delete forwarding rule
            gceOperations.deleteForwardingRule(gceLoadBalancerConfiguration.getForwardingRuleName());
            //delete target pool from GCE
            gceOperations.deleteTargetPool(gceLoadBalancerConfiguration.getTargetPoolName());
            //delete health check from GCE
            gceOperations.deleteHealthCheck(gceLoadBalancerConfiguration.getHealthCheckName());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not delete configuration for cluster " + clusterId);
            }
            throw new LoadBalancerExtensionException();
        }

    }

    /**
     * This method is used to create target pools, forwarding rule and a health check related to a cluster
     * in IaaS side according to new topology
     */
    private void createConfigurationForCluster(String clusterId) throws LoadBalancerExtensionException {

        try {

            GCELoadBalancerConfiguration gceLoadBalancerConfiguration = clusterToLoadBalancerConfigurationMap.get(clusterId);

            //create a health check
            gceOperations.createHealthCheck(gceLoadBalancerConfiguration.getHealthCheckName());

            gceOperations.createFirewallRule();

            //crate a target pool in GCE
            gceOperations.createTargetPool(gceLoadBalancerConfiguration.getTargetPoolName(),
                    gceLoadBalancerConfiguration.getHealthCheckName());

            //add instances to target pool
            gceOperations.addInstancesToTargetPool(gceLoadBalancerConfiguration.getMemberList(),
                    gceLoadBalancerConfiguration.getTargetPoolName());

            //create forwarding rules in GCE
            List<Integer> ipList = gceLoadBalancerConfiguration.getIpList();
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
            gceOperations.createForwardingRule(gceLoadBalancerConfiguration.getForwardingRuleName(),
                    gceLoadBalancerConfiguration.getTargetPoolName(), protocol, portRange);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create configuration for cluster " + clusterId);
            }
            throw new LoadBalancerExtensionException();
        }

    }


    @Override
    public void start() throws LoadBalancerExtensionException {
        //topology has completed

        log.info("GCE Load balancer instance started");

    }

    @Override
    public void stop() throws LoadBalancerExtensionException {

        log.info("GCE Load Balancer is stopping");

        //iterate through hashmap and remove all

        //deleteAll();

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