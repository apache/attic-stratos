package org.apache.stratos.messaging.event.health.stat;

import org.apache.stratos.messaging.event.Event;

/**
 * Created by pranavan on 8/9/15.
 */
public class MemberCurveFinderOfMemoryConsumptionEvent extends Event {
    private final String memberId;
    private final String clusterId;
    private final String clusterInstanceId;
    private final String networkPartitionId;
    private final double value;
    /**
     * Curve finder co-efficients
     */
    private final double a;
    private final double b;
    private final double c;


    public MemberCurveFinderOfMemoryConsumptionEvent(String memberId, String clusterId, String clusterInstanceId, String networkPartitionId, double value, double a, double
            b, double c) {
        this.memberId = memberId;
        this.clusterId = clusterId;
        this.clusterInstanceId = clusterInstanceId;
        this.networkPartitionId = networkPartitionId;
        this.value = value;
        this.a = a;
        this.b = b;
        this.c = c;
    }


    public String getMemberId() {
        return memberId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    public double getValue() {
        return value;
    }

    public double getA() {
        return a;
    }

    public double getB() {
        return b;
    }

    public double getC() {
        return c;
    }
}
