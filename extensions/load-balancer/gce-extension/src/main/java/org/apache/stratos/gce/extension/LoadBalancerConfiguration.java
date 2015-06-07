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

import com.google.api.services.compute.model.TargetPool;

import java.util.ArrayList;
import java.util.List;

/**
 * Since GCE have separate target pools and forwarding rules, we need
 * a class to concatenate target pools and forwarding rules which belongs to
 * one cluster in stratos.
 * This class is used to hold the configuration for a load balancer
 * Each cluster will have one object from this class
 */
public class LoadBalancerConfiguration {


    //A load balancer must have a target pool
    private TargetPool targetPool;
    //A load balancer can have one or more forwarding rules(set of ports to be forwarded)
    private List<Integer> forwardingRulesList;
    //cluster ID from stratos side
    private String clusterID;
    //Whether we have executed this configuration in GCE or not
    private boolean isRunning = false;


    public LoadBalancerConfiguration(String clusterID, TargetPool targetPool) {
        this.clusterID = clusterID;
        this.targetPool = targetPool;
        forwardingRulesList = new ArrayList<Integer>();
    }

    public void addForwardingRule(Integer forwardingRule) {
        forwardingRulesList.add(forwardingRule);
    }

    public List getForwardingRulesList() {
        return forwardingRulesList;
    }

    public TargetPool getTargetPool() {
        return targetPool;
    }


    public String getClusterID() {
        return clusterID;
    }

    public void setStatus(boolean status) {
        isRunning = status;
    }

    public boolean getStatus() {
        return isRunning;
    }

}