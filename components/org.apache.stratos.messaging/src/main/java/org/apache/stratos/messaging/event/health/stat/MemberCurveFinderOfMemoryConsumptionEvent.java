package org.apache.stratos.messaging.event.health.stat;

import org.apache.stratos.messaging.event.Event;

/**
 * Created by pranavan on 8/9/15.
 */
public class MemberCurveFinderOfMemoryConsumptionEvent extends Event {
    private final String clusterInstanceId;
    private final String memberId;
    private final double timeStamp;
    /**
     * Curve finder co-efficients
     */
    private final float a;
    private final float b;
    private final float c;


    public MemberCurveFinderOfMemoryConsumptionEvent(String clusterInstanceId, String memberId, double timeStamp, float a, float
            b, float c) {
        this.clusterInstanceId = clusterInstanceId;
        this.memberId = memberId;
        this.timeStamp = timeStamp;
        this.a = a;
        this.b = b;
        this.c = c;
    }


    public String getMemberId() {
        return memberId;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    public double getTimeStamp() {
        return timeStamp;
    }

    public float getA() {
        return a;
    }

    public float getB() {
        return b;
    }

    public float getC() {
        return c;
    }
}
