package org.apache.stratos.messaging.event.health.stat;

import org.apache.stratos.messaging.event.Event;

/**
 * Created by pranavan on 8/9/15.
 */
public class MemberCurveFinderOfMemoryConsumptionEvent extends Event {
    private final String clusterInstanceId;
    private final String memberId;
    private final float value;

    public MemberCurveFinderOfMemoryConsumptionEvent(String clusterInstanceId, String memberId, float value) {
        this.clusterInstanceId = clusterInstanceId;
        this.memberId = memberId;
        this.value = value;
    }


    public String getMemberId() {
        return memberId;
    }

    public float getValue() {
        return value;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

}
