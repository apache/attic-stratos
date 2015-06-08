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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Since GCE have separate target pools and forwarding rules, we need
 * a class to concatenate target pools and forwarding rules which belongs to
 * one cluster in stratos.
 * This class is used to hold the configuration for a load balancer
 * Each cluster will have one object from this class
 */
public class LoadBalancerConfiguration {


    //A load balancer must have a target pool(set of instances)
    private List<String> instancesList;
    //A load balancer can have one or more forwarding rules(set of ports to be forwarded)
    //we create a map to store the port IP and forwarding rule name
    private HashMap<Integer,String> ipToForwardingRuleNameMap;
    //cluster ID from stratos side
    private String clusterID;
    private String targetPoolName;
    //Whether we have executed this configuration in GCE or not
    private boolean isRunning = false;


    public LoadBalancerConfiguration(String clusterID, List<String> instancesList,
                                     HashMap<Integer,String> ipToForwardingRuleNameMap) {
        this.clusterID = clusterID;
        this.instancesList = instancesList;
        this.ipToForwardingRuleNameMap = ipToForwardingRuleNameMap;
    }

    public void setipToForwardingRuleNameMap(HashMap<Integer,String> ipToForwardingRuleNameMap) {
        this.ipToForwardingRuleNameMap = ipToForwardingRuleNameMap;
    }

    public void addForwardingRule(int ip, String forwardingRuleName){
        this.ipToForwardingRuleNameMap.put(ip,forwardingRuleName);
    }

    public Set<Integer> getPorts() {
        return ipToForwardingRuleNameMap.keySet();
    }

    public Collection<String> getForwardingRuleNames(){
        return ipToForwardingRuleNameMap.values();
    }

    public List<String> getInstancesList() {
        return instancesList;
    }

    public void setInstancesList(List<String> instancesList) {
        this.instancesList = instancesList;
    }


    public String getClusterID() {
        return clusterID;
    }

    public void setStatus(boolean status) {
        this.isRunning = status;
    }

    public void setTargetPoolName(String targetPoolName){
        this.targetPoolName = targetPoolName;
    }

    public String getTargetPoolName(){
        return targetPoolName;
    }

    public boolean getStatus() {
        return isRunning;
    }

}