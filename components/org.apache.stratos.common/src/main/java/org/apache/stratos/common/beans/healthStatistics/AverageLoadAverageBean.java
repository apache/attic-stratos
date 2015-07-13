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

package org.apache.stratos.common.beans.healthStatistics;


public class AverageLoadAverageBean {
    private static final long serialVersionUID = -7788619177798333712L;

    private String clusterId;
    private String clusterInstanceId;
    private Long timeStamp;
    private Double memberAverageLoadAverage;
    private String memberId;
    private String networkPartitionId;

    public AverageLoadAverageBean(String clusterId, String clusterInstanceId, Long timeStamp, Double memberAverageLoadAverage, String memberId, String networkPartitionId) {
        this.clusterId = clusterId;
        this.clusterInstanceId = clusterInstanceId;
        this.timeStamp = timeStamp;
        this.memberAverageLoadAverage = memberAverageLoadAverage;
        this.memberId = memberId;
        this.networkPartitionId = networkPartitionId;


    }
    
    

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    public void setClusterInstanceId(String clusterInstanceId) {
        this.clusterInstanceId = clusterInstanceId;
    }

    public Double getMemberAverageLoadAverage() {
        return memberAverageLoadAverage;
    }

    public void setMemberAverageLoadAverage(Double memberAverageLoadAverage) {
        this.memberAverageLoadAverage = memberAverageLoadAverage;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }
    
}
