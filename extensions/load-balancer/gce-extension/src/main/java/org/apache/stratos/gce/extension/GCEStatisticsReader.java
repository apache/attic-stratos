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


import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;

/**
 * GCE extension statistics reader class.
 */
public class GCEStatisticsReader implements LoadBalancerStatisticsReader {

    private TopologyProvider topologyProvider;
    private String clusterInstanceId;

    public GCEStatisticsReader(TopologyProvider topologyProvider){
        this.topologyProvider = topologyProvider;
        this.clusterInstanceId = System.getProperty(StratosConstants.
                CLUSTER_INSTANCE_ID, StratosConstants.NOT_DEFINED);

    }

    @Override
    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    @Override
    public int getInFlightRequestCount(String clusterId) {

        return 0;
    }
}