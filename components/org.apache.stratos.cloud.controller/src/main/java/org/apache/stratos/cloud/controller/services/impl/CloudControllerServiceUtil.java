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

package org.apache.stratos.cloud.controller.services.impl;

import com.google.common.net.InetAddresses;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.pojo.ApplicationContext;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyManager;
import org.apache.stratos.cloud.controller.statistics.publisher.CloudControllerPublisherFactory;
import org.apache.stratos.cloud.controller.statistics.publisher.MemberStatusPublisher;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.client.AutoscalerServiceClient;
import org.apache.stratos.common.statistics.publisher.StatisticsPublisherType;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;

import java.rmi.RemoteException;
import java.util.Properties;

/**
 * Cloud controller service utility methods.
 */
public class CloudControllerServiceUtil {

    private static final Log log = LogFactory.getLog(CloudControllerServiceUtil.class);

    public static Iaas buildIaas(IaasProvider iaasProvider) throws InvalidIaasProviderException {
        return iaasProvider.getIaas();
    }

    /**
     * Update the topology, publish statistics to DAS, remove member context
     * and persist cloud controller context.
     *
     * @param memberContext MemberContext of the Member
     */
    public static void executeMemberTerminationPostProcess(MemberContext memberContext) {
        if (memberContext == null) {
            return;
        }
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(memberContext.getCartridgeType());
        String applicationUuid = service.getCluster(memberContext.getClusterId()).getAppId();
        ApplicationContext applicationContext = null;
        try {
            applicationContext = AutoscalerServiceClient.getInstance().getApplication(applicationUuid);
        } catch (RemoteException e) {
            String message = String.format("Error while getting the application context for [applicationUuid] %s" +
                    applicationUuid);
            log.error(message, e);
        }
        String applicationId = applicationContext.getApplicationId();
        int tenantId = applicationContext.getTenantId();
        String clusterAlias = CloudControllerUtil.getAliasFromClusterId(memberContext.getClusterId());

        String partitionId = memberContext.getPartition() == null ? null : memberContext.getPartition().getUuid();

        // Update the topology
        TopologyBuilder.handleMemberTerminated(memberContext.getCartridgeType(),
                memberContext.getClusterId(), memberContext.getNetworkPartitionId(),
                partitionId, memberContext.getMemberId());
        //member terminated time
        Long timestamp = System.currentTimeMillis();
        //publishing member status to DAS.
        MemberStatusPublisher memStatusPublisher = CloudControllerPublisherFactory.
                createMemberStatusPublisher(StatisticsPublisherType.WSO2DAS);
        if (memStatusPublisher.isEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Publishing Member Status to DAS");
            }
            memStatusPublisher.publish(timestamp,
                    tenantId,
                    applicationId,
                    memberContext.getClusterId(),
                    clusterAlias,
                    memberContext.getClusterInstanceId(),
                    memberContext.getCartridgeType(),
                    memberContext.getNetworkPartitionId(),
                    memberContext.getPartition().getId(),
                    memberContext.getMemberId(),
                    MemberStatus.Terminated.toString());
        } else {
            log.warn("Member Status Publisher is not enabled");
        }

        // Remove member context
        CloudControllerContext.getInstance().removeMemberContext(memberContext.getClusterId(),
                memberContext.getMemberId());

        // Persist cloud controller context
        CloudControllerContext.getInstance().persist();
    }

    public static boolean isValidIpAddress(String ip) {
        return InetAddresses.isInetAddress(ip);
    }

    public static IaasProvider validatePartitionAndGetIaasProvider(Partition partition, IaasProvider iaasProvider)
            throws InvalidPartitionException {
        if (iaasProvider != null) {
            // if this is a IaaS based partition
            Iaas iaas = iaasProvider.getIaas();
            PartitionValidator validator = iaas.getPartitionValidator();
            validator.setIaasProvider(iaasProvider);
            Properties partitionProperties = CloudControllerUtil.toJavaUtilProperties(partition.getProperties());
            iaasProvider = validator.validate(partition, partitionProperties);
            return iaasProvider;

        } else {
            String msg = "Partition is not valid: [partition-id] " + partition.getUuid();
            log.error(msg);
            throw new InvalidPartitionException(msg);
        }
    }

    public static boolean validatePartition(Partition partition, IaasProvider iaasProvider)
            throws InvalidPartitionException {
        validatePartitionAndGetIaasProvider(partition, iaasProvider);
        return true;
    }
}
