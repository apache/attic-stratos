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

public class AverageMemoryConsumptionBean {
    private static final long serialVersionUID = -7788619177798333711L;

    private String clusterId;
    private Double memberAverageMemoryConsumption;
    private Long timeStamp;
    private String memberId;

    public AverageMemoryConsumptionBean(String clusterId, Double memberAverageMemoryConsumption, Long timeStamp, String memberId) {
        this.clusterId = clusterId;
        this.memberAverageMemoryConsumption = memberAverageMemoryConsumption;
        this.timeStamp = timeStamp;
        this.memberId = memberId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public Double getMemberAverageMemoryConsumption() {
        return memberAverageMemoryConsumption;
    }

    public void setMemberAverageMemoryConsumption(Double memberAverageMemoryConsumption) {
        this.memberAverageMemoryConsumption = memberAverageMemoryConsumption;
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

}

