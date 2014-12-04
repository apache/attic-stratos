/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer.context;

import org.apache.stratos.load.balancer.context.map.AlgorithmContextMap;

import java.util.concurrent.locks.Lock;

/**
 * Algorithm context is used for identifying the cluster and its current member for executing load balancing algorithms.
 * Key: service name, cluster id
 */
public class AlgorithmContext {
    private String serviceName;
    private String clusterId;

    public AlgorithmContext(String serviceName, String clusterId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        Lock lock = null;
        try {
            lock = AlgorithmContextMap.getInstance().acquireCurrentMemberIndexLock();
            AlgorithmContextMap.getInstance().putCurrentMemberIndex(serviceName, clusterId, 0);
        } finally {
            if(lock != null) {
                AlgorithmContextMap.getInstance().releaseCurrentMemberIndexLock(lock);
            }
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public int getCurrentMemberIndex() {
        return AlgorithmContextMap.getInstance().getCurrentMemberIndex(getServiceName(), getClusterId());
    }

    public void setCurrentMemberIndex(int currentMemberIndex) {
        Lock lock = null;
        try {
            lock = AlgorithmContextMap.getInstance().acquireCurrentMemberIndexLock();
            AlgorithmContextMap.getInstance().putCurrentMemberIndex(getServiceName(), getClusterId(), currentMemberIndex);
        } finally {
            if(lock != null) {
                AlgorithmContextMap.getInstance().releaseCurrentMemberIndexLock(lock);
            }
        }
    }
}
