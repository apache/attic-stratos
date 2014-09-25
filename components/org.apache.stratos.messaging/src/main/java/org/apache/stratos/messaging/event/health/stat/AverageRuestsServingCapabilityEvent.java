package org.apache.stratos.messaging.event.health.stat;

/**
 * Created by asiri on 8/10/14.
 */
import org.apache.stratos.messaging.event.Event;
public class AverageRuestsServingCapabilityEvent extends  Event{
    private final String networkPartitionId;
    private final String clusterId;
    private final float value;

    public AverageRuestsServingCapabilityEvent(String networkPartitionId, String clusterId, float value) {
        this.networkPartitionId = networkPartitionId;
        this.clusterId = clusterId;
        this.value = value;

    }
    public String getClusterId() {
        return clusterId;
    }

    public float getValue() {
        return value;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }


}
