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

import java.util.List;

/**
 * Since GCE have separate target pools and forwarding rules, we need
 * a class to concatenate target pools and forwarding rules which belongs to
 * one cluster in stratos.
 * This class is used to hold the configuration for a load balancer
 * Each cluster will have one object from this class
 */
public class GCELoadBalancerConfiguration {


    //A load balancer must have set of instances
    private List<String> instancesList;
    //A load balancer can have one IPs to be forwarded
    private List<Integer> ipList;
    //cluster ID from stratos side
    private String clusterID;
    private String forwardingRuleName;
    private String targetPoolName;
    private String healthCheckName;


    public GCELoadBalancerConfiguration(String clusterID, List<String> instancesList,
                                        List<Integer> ipList) {
        this.clusterID = clusterID;
        this.instancesList = instancesList;
        this.ipList = ipList;
    }

    public List<Integer> getIpList() {
        return ipList;
    }

    public void setIpList(List<Integer> ipList) {
        this.ipList = ipList;
    }

    public String getForwardingRuleName() {
        return forwardingRuleName;
    }

    public void setForwardingRuleName(String forwardingRuleName) {
        this.forwardingRuleName = forwardingRuleName;
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

    public String getTargetPoolName() {
        return targetPoolName;
    }

    public void setTargetPoolName(String targetPoolName) {
        this.targetPoolName = targetPoolName;
    }

    public String getHealthCheckName() {
        return healthCheckName;
    }

    public void setHealthCheckName(String healthCheckName) {
        this.healthCheckName = healthCheckName;
    }
}