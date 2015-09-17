/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cloud.controller.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.common.statistics.publisher.ThriftStatisticsPublisher;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Publishing member status to DAS.
 */
public class DASMemberStatusPublisher extends ThriftStatisticsPublisher implements MemberStatusPublisher {

    private static final Log log = LogFactory.getLog(DASMemberStatusPublisher.class);
    private static final String DATA_STREAM_NAME = "member_lifecycle";
    private static final String VERSION = "1.0.0";
    private static final String DAS_THRIFT_CLIENT_NAME = "das";
    private ExecutorService executorService;

    public DASMemberStatusPublisher() {
        super(createStreamDefinition(), DAS_THRIFT_CLIENT_NAME);
        executorService = StratosThreadPool.getExecutorService("cloud.controller.stats.publisher.thread.pool", 10);
    }

    private static StreamDefinition createStreamDefinition() {
        try {
            // Create stream definition
            StreamDefinition streamDefinition = new StreamDefinition(DATA_STREAM_NAME, VERSION);
            streamDefinition.setNickName("Member Lifecycle");
            streamDefinition.setDescription("Member Lifecycle");
            List<Attribute> payloadData = new ArrayList<Attribute>();

            // Set payload definition
            payloadData.add(new Attribute(CloudControllerConstants.TIMESTAMP_COL, AttributeType.LONG));
            payloadData.add(new Attribute(CloudControllerConstants.TENANT_ID_COL, AttributeType.INT));
            payloadData.add(new Attribute(CloudControllerConstants.APPLICATION_ID_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.CLUSTER_ID_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.CLUSTER_ALIAS_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.CLUSTER_INSTANCE_ID_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.SERVICE_NAME_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.NETWORK_PARTITION_ID_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.PARTITION_ID_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.MEMBER_ID_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.MEMBER_STATUS_COL, AttributeType.STRING));
            streamDefinition.setPayloadData(payloadData);
            return streamDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Could not create stream definition", e);
        }
    }

    /**
     * publishing Member Status to DAS.
     *
     * @param timestamp          Status changed time
     * @param applicationId      Application Id
     * @param clusterId          Cluster Id
     * @param clusterAlias       Cluster Alias
     * @param clusterInstanceId  Cluster Instance Id
     * @param networkPartitionId Network Partition Id
     * @param partitionId        Partition Id
     * @param serviceName        Service Name
     * @param memberId           Member Id
     * @param status             Member Status
     * @parm tenantId            Tenant Id
     */
    @Override
    public void publish(final Long timestamp, final int tenantId, final String applicationId, final String clusterId,
                        final String clusterAlias, final String clusterInstanceId,
                        final String serviceName, final String networkPartitionId, final String partitionId,
                        final String memberId, final String status) {

        Runnable publisher = new Runnable() {
            @Override
            public void run() {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Publishing member status: [timestamp] %d [tenant_id] %d " +
                                    "[application_id] %s [cluster_id] %s [cluster_alias] %s" +
                                    "[cluster_instance_id] %s [service_name] %s " +
                                    "[network_partition_id] %s [partition_id] %s " +
                                    "[member_id] %s [member_status] %s ",
                            timestamp, tenantId, applicationId, clusterId, clusterAlias, clusterInstanceId, serviceName,
                            networkPartitionId, partitionId, memberId, status));
                }
                //adding payload data
                List<Object> payload = new ArrayList<Object>();
                payload.add(timestamp);
                payload.add(Integer.valueOf(tenantId));
                payload.add(applicationId);
                payload.add(clusterId);
                payload.add(clusterAlias);
                payload.add(clusterInstanceId);
                payload.add(serviceName);
                payload.add(networkPartitionId);
                payload.add(partitionId);
                payload.add(memberId);
                payload.add(status);
                DASMemberStatusPublisher.super.publish(payload.toArray());
            }
        };
        executorService.execute(publisher);
    }

}
