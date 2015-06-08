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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        log.info("Starting GCE Load balancer instance");

        //iterate through clusterToLoadBalancerConfigurationMap
        //for each cluster{
        //if Load balancer status == not running -->
        //      execute commands through GCE API for start the load balancer
        //}

        log.info("GCE Load balancer instance started");


    }

    @Override
    public void stop() throws LoadBalancerExtensionException {

        log.info("GCE Load Balancer is stopping");
        //iterate through hashmap and remove all
    }

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
                    List<String> instancesList = loadBalancerConfiguration.getInstancesList();


                    for (Member member : cluster.getMembers()) {

                        //TODO:check instances(members) in target pool and update

                        //add forwarding rules(Ports to be forwarded)

                        for (Object port : member.getPorts()) {


                        }
                    }


                } else {
                    //doesn't have a loadBalancerConfiguration object. So crate a new one and add to hash map

                    List<String> instancesList = new ArrayList<String>();

                    List<Integer> forwardingRulesList = new ArrayList<Integer>();

                    for (Member member : cluster.getMembers()) {

                        //TODO:set instances(members) to targetPool

                        //add forwarding rules(Ports to be forwarded)

                        for (Object port : member.getPorts()) {
                            if (!forwardingRulesList.contains(((Port) port).getValue())) { //if port is not in list
                                forwardingRulesList.add(((Port) port).getValue());
                            }
                        }


                    }

                    LoadBalancerConfiguration loadBalancerConfiguration = new LoadBalancerConfiguration(
                            cluster.getClusterId(), instancesList);

                    loadBalancerConfiguration.setForwardingRulesList(forwardingRulesList);

                    //get forwarding rules from topology and set forwarding rules to
                    // LoadBalancerConfiguration object

                    clusterToLoadBalancerConfigurationMap.put(cluster.getClusterId(), loadBalancerConfiguration);

                }

            }
        }

        return true;
    }

    @Override
    public void reload() throws LoadBalancerExtensionException {

        //iterate through hash map
        //find what needs to be changed
        //execute

    }
}