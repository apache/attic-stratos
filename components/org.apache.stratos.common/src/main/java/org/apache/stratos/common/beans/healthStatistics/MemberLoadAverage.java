/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stratos.common.beans.healthStatistics;

/**
 *
 * @author NLiyanage
 */
public class MemberLoadAverage {
    private static final long serialVersionUID = -7788619177798333712L;

    private String clusterId;
    private String clusterInstanceId;
    private Integer timeStamp;
    private Double memberAverageLoadAverage;
    private String memberId;
    private String networkPartitionId;

    public MemberLoadAverage(String clusterId, String clusterInstanceId, Integer timeStamp, Double memberAverageLoadAverage, String memberId, String networkPartitionId) {
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

    public Integer getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Integer timeStamp) {
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
