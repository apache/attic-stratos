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
     * We create one load balancer per cluster(stratos side).
     * A Load balancer has one target pool and set of forwarding rules
     * This hash map is used to hold cluster IDs and corresponding Load balancer configuration
     * So one cluster will have one loadBalancerConfiguration object
     */
    private HashMap<String, LoadBalancerConfiguration> clusterToLoadBalancerConfigurationMap;

    //protocol should be TCP or UDP
    private String protocol = "TCP";

    public GCELoadBalancer() {
        try {
            gceOperations = new GCEOperations();
            clusterToLoadBalancerConfigurationMap = new HashMap<String, LoadBalancerConfiguration>();

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
        while (iterator.hasNext()){ //for each Load balancer configuration

            Map.Entry clusterIDLoadBalancerConfigurationPair = (Map.Entry)iterator.next();

            String clusterID = ((String) clusterIDLoadBalancerConfigurationPair.getKey());
            LoadBalancerConfiguration loadBalancerConfiguration =
                    ((LoadBalancerConfiguration) clusterIDLoadBalancerConfigurationPair.getValue());

            if(!loadBalancerConfiguration.getStatus()) { //if the load balancer is NOT already running


                //crate a target pool in GCE
                gceOperations.createTargetPool(loadBalancerConfiguration.getTargetPoolName());

                //add instances to target pool
                gceOperations.addInstancesToTargetPool(loadBalancerConfiguration.getInstancesList(),
                        loadBalancerConfiguration.getTargetPoolName());


                //create forwarding rules in GCE
                for (String forwardingRuleName : loadBalancerConfiguration.getForwardingRuleNames()) { //for each port
                    //create a forwarding rule in GCE
                    gceOperations.createForwardingRule(forwardingRuleName,
                            loadBalancerConfiguration.getTargetPoolName(), protocol);
                }

                //set status to running
                loadBalancerConfiguration.setStatus(true);
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
            LoadBalancerConfiguration loadBalancerConfiguration =
                    ((LoadBalancerConfiguration) clusterIDLoadBalancerConfigurationPair.getValue());

            if (loadBalancerConfiguration.getStatus()) { //if the load balancer is  already running

                //delete forwarding rules from GCE
                for(String forwardingRuleName : loadBalancerConfiguration.getForwardingRuleNames()){
                    gceOperations.deleteForwardingRule(forwardingRuleName);
                }

                //delete target pool from GCE
                gceOperations.deleteTargetPool(loadBalancerConfiguration.getTargetPoolName());

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

        //printing topology value as a xml for testing purposes
       /* XStream xstream = new XStream(new Sun14ReflectionProvider(
                new FieldDictionary(new ImmutableFieldKeySorter())),
                new DomDriver("utf-8"));
        log.info(xstream.toXML(topology));
        */

        for (Service service : topology.getServices()) {
            for (Cluster cluster : service.getClusters()) { //for each cluster
                //we create one load balancer per cluster
                //Only create configuration object. Not execute.

                //check whether this cluster has a forwarding rule configuration or not
                if (clusterToLoadBalancerConfigurationMap.containsKey(cluster.getClusterId())) {

                    //It has a loadBalancer configured. Take it and update it as the given topology.

                    //get load balancer configuration
                    LoadBalancerConfiguration loadBalancerConfiguration = clusterToLoadBalancerConfigurationMap.
                            get(cluster.getClusterId());

                    //check and update
                    List<String> updatedInstancesList = new ArrayList<String>();
                    HashMap<Integer,String> updatedIpToForwardingRuleNameMap = new HashMap<Integer, String>();


                    for (Member member : cluster.getMembers()) {

                        //TODO:check instances(members) in instance list and update
                      /*  EX:
                        if(!updatedInstancesList.contains(member.getMemberName())){
                            updatedInstancesList.add(member.getMemberName);
                        }
                        */

                        //checking for forwarding rules and updating
                        for (Object port : member.getPorts()) {

                            int portValue = ((Port) port).getValue();

                            //if not present update it
                            if (!updatedIpToForwardingRuleNameMap.containsKey(portValue)){ //if port is not in list
                                //create a new forwarding rule name
                                String forwardingRuleName = forwardingRuleNameCreator(portValue,cluster.getClusterId());
                                //put the forwarding rule to map
                                updatedIpToForwardingRuleNameMap.put(portValue, forwardingRuleName);
                            }

                        }
                    }

                    //set new forwarding rules and instances list
                    loadBalancerConfiguration.setIPToForwardingRuleNameMap(updatedIpToForwardingRuleNameMap);
                    loadBalancerConfiguration.setInstancesList(updatedInstancesList);

                    if(loadBalancerConfiguration.getTargetPoolName() == null){ //this does not have a target pool name
                        //set target pool name
                        String targetPoolName = targetPoolNameCreator(cluster.getClusterId());
                        loadBalancerConfiguration.setTargetPoolName(targetPoolName);
                    }


                } else {
                    //doesn't have a loadBalancerConfiguration object. So crate a new one and add to hash map

                    List<String> instancesList = new ArrayList<String>();
                    HashMap<Integer,String> ipToForwardingRuleNameMap = new HashMap<Integer, String>();

                    for (Member member : cluster.getMembers()) {

                        //TODO:set instances(members) to instances list
                        //instancesList.add(member.getMemberName);

                        //add forwarding rules(Ports to be forwarded)
                        for (Object port : member.getPorts()) {
                            int portValue = ((Port) port).getValue();
                            if (!ipToForwardingRuleNameMap.containsKey(portValue)) { //if port is not in list
                                //create a new forwarding rule name
                                String forwardingRuleName = forwardingRuleNameCreator(portValue,cluster.getClusterId());
                                //put the forwarding rule to map
                                ipToForwardingRuleNameMap.put(portValue,forwardingRuleName);
                            }
                        }

                    }

                    LoadBalancerConfiguration loadBalancerConfiguration = new LoadBalancerConfiguration(
                            cluster.getClusterId(), instancesList, ipToForwardingRuleNameMap);
                    if(loadBalancerConfiguration.getTargetPoolName() == null){ //this does not have a target pool name
                        //set target pool name
                        String targetPoolName = targetPoolNameCreator(cluster.getClusterId());
                        loadBalancerConfiguration.setTargetPoolName(targetPoolName);
                    }

                    clusterToLoadBalancerConfigurationMap.put(cluster.getClusterId(), loadBalancerConfiguration);

                }

            }
        }

        return true;
    }

    /**
     *
     * @throws LoadBalancerExtensionException
     */
    @Override
    public void reload() throws LoadBalancerExtensionException {

        //iterate through hash map
        //find what needs to be changed
        //execute

    }

    private String targetPoolNameCreator(String clusterID){
        //create a valid target pool name by using cluster ID
        //remove spaces, make all to lower case, replace all "." --> "-"
        //add name prefix
        String targetPoolName = GCEContext.getInstance().getNamePrefix().toLowerCase() + "-" +
                clusterID.trim().toLowerCase().replace(".","-");
        //length should be les than 62 characters
        if(targetPoolName.length()>=62){
            targetPoolName = targetPoolName.substring(0,62);
        }
        return  targetPoolName;
    }

    private String forwardingRuleNameCreator(int port,String clusterID){
        String forwardingRuleName = GCEContext.getInstance().getNamePrefix() + "-" +
                Integer.toString(port) + "-" + clusterID;
        //length should be les than 62 characters
        if(forwardingRuleName.length()>=62){
            forwardingRuleName = forwardingRuleName.substring(0,62);
        }
        return  forwardingRuleName;
    }
}