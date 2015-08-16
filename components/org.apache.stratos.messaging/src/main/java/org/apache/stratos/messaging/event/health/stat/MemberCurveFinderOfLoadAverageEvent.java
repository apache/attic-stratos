package org.apache.stratos.messaging.event.health.stat;

import org.apache.stratos.messaging.event.Event;

/**
 * Created by pranavan on 8/9/15.
 */
public class MemberCurveFinderOfLoadAverageEvent extends Event {
    private final String clusterId;
    private final String clusterInstanceId;
    private final String memberId;
    private final double value;
    /**
     * Curve finder co-efficients
     */
    private final double a;
    private final double b;
    private final double c;


    public MemberCurveFinderOfLoadAverageEvent(String clusterId, String clusterInstanceId, String memberId, double value, double a, double b, double c) {
        this.clusterId = clusterId;
        this.clusterInstanceId = clusterInstanceId;
        this.memberId = memberId;
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
