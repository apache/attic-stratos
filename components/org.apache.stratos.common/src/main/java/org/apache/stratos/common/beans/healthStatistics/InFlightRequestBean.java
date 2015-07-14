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


public class InFlightRequestBean {

    private static final long serialVersionUID = -7788619177798333712L;

    private String clusterId;
    private Long timeStamp;
    private Double inFlightRequestCount;

    public InFlightRequestBean(String clusterId, Long timeStamp, Double inFlightRequestCount) {
        this.clusterId = clusterId;
        this.timeStamp = timeStamp;
        this.inFlightRequestCount = inFlightRequestCount;
    }

    
    

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public Double getInFlightRequestCount() {
        return inFlightRequestCount;
    }

    public void setInFlightRequestCount(Double inFlightRequestCount) {
        this.inFlightRequestCount = inFlightRequestCount;
    }


    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

}
