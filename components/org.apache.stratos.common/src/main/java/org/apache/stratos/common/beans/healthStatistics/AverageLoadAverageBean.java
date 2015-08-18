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

/*
* This bean class may use to create the data transfer objects for Load Average data of clusters and members.
* */
public class AverageLoadAverageBean {

    private static final long serialVersionUID = -7788619177798333712L;

    private final String clusterId;
    private final Long timeStamp;
    private final Double memberAverageLoadAverage;
    private final String memberId;

    public AverageLoadAverageBean(String clusterId, Long timeStamp, Double memberAverageLoadAverage, String memberId) {
        this.clusterId = clusterId;
        this.timeStamp = timeStamp;
        this.memberAverageLoadAverage = memberAverageLoadAverage;
        this.memberId = memberId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public Double getMemberAverageLoadAverage() {
        return memberAverageLoadAverage;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public String getMemberId() {
        return memberId;
    }
    
}
