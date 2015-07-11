package org.apache.stratos.common.beans.healthStatistics;

/**
 * Created by dk on 11/07/2015.
 */
public class AverageMemoryConsumption {
    private static final long serialVersionUID = -7788619177798333711L;

    private String clusterId;
    private String clusterInstanceId;
    private Double memberAverageMemoryConsumption;
    private Integer timeStamp;
    private String memberId;
    private String networkPartitionId;

    public AverageMemoryConsumption(String clusterId, String clusterInstanceId, Double memberAverageMemoryConsumption, Integer timeStamp, String memberId, String networkPartitionId) {
        this.clusterId = clusterId;
        this.clusterInstanceId = clusterInstanceId;
        this.memberAverageMemoryConsumption = memberAverageMemoryConsumption;
        this.timeStamp = timeStamp;
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

    public Double getMemberAverageMemoryConsumption() {
        return memberAverageMemoryConsumption;
    }

    public void setMemberAverageMemoryConsumption(Double memberAverageMemoryConsumption) {
        this.memberAverageMemoryConsumption = memberAverageMemoryConsumption;
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

