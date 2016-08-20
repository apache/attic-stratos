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
package org.apache.stratos.rest.endpoint.test;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.domain.LoadBalancingIPType;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.CompleteApplicationsEvent;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.event.topology.MemberInitializedEvent;
import org.apache.stratos.messaging.listener.topology.MemberInitializedEventListener;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.apache.stratos.rest.endpoint.api.StratosApiV41Utils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UndeployApplicationTest {
    public static final String APPLICATION_ID = "test-app-1";
    public static final String APPLICATION_ALIAS = "test-app-alias-1";
    public static final String APPLICATION_INSTANCE_ID = "test-app-instance-1";
    public static final String SERVICE_NAME = "test-service-1";
    public static final String CLUSTER_ID = "test-cluster-1";
    public static final String CLUSTER_INSTANCE_ID = "test-cluster-instance-1";
    public static final String DEPLOYMENT_POLICY_ID = "test-deployment-policy-1";
    public static final String AUTOSCALE_POLICY_ID = "test-autoscale-policy-1";
    public static final String NETWORK_PARTITION_ID = "test-network-partition-1";
    public static final String PARTITION_ID = "test-partition-1";
    public static final String TEST_MEMBER_ID_1 = "test-member-1";
    public static final String TEST_MEMBER_INSTANCE_ID_1 = "test-member-1";
    private static final Log log = LogFactory.getLog(UndeployApplicationTest.class);
    private final ExecutorService executorService = StratosThreadPool
            .getExecutorService("org.apache.stratos.rest.endpoint.test", 20);
    private BrokerService broker;
    private boolean memberInitializedEventPublished = false;

    private static String getResourcesFolderPath() {
        String path = UndeployApplicationTest.class.getResource("/").getPath();
        return StringUtils.removeEnd(path, File.separator);
    }

    @BeforeClass
    public static void setUp() {
        // Set jndi.properties.dir system property for initializing event receivers
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
    }

    private void initializeActiveMQ() {
        try {
            log.info("Initializing ActiveMQ...");
            broker = new BrokerService();
            broker.setDataDirectory(UndeployApplicationTest.class.getResource("/").getPath() +
                    File.separator + ".." + File.separator + "activemq-data");
            broker.setBrokerName("testBroker");
            broker.addConnector("tcp://localhost:61617");
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize ActiveMQ", e);
        }
    }

    private void startActiveMQ() {
        try {
            long time1 = System.currentTimeMillis();
            broker.start();
            long time2 = System.currentTimeMillis();
            log.info(String.format("ActiveMQ started in %d sec", (time2 - time1) / 1000));
        } catch (Exception e) {
            throw new RuntimeException("Could not start ActiveMQ", e);
        }
    }

    private void stopActiveMQ() {
        try {
            broker.stop();
        } catch (Exception e) {
            throw new RuntimeException("Could not stop ActiveMQ", e);
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
        }
    }

    @Test(timeout = 60000)
    public void testSubscriberReconnection() throws Exception {
        initializeActiveMQ();
        startActiveMQ();

        TopologyEventReceiver topologyEventReceiver = new TopologyEventReceiver();
        topologyEventReceiver.setExecutorService(executorService);
        topologyEventReceiver.execute();

        ApplicationsEventReceiver applicationsEventReceiver = new ApplicationsEventReceiver();
        applicationsEventReceiver.setExecutorService(executorService);
        applicationsEventReceiver.execute();

        publishCompleteTopologyEvent();
        publishCompleteApplicationsEvent();

        while (!TopologyManager.getTopology().isInitialized() || !ApplicationManager.getApplications()
                .isInitialized()) {
            log.info("Waiting until topology and application model is initialized...");
            sleep(1000);
        }

        assertFalse("Member initialized status check failed", StratosApiV41Utils.hasMembersInitialized(APPLICATION_ID));

        topologyEventReceiver.addEventListener(new MemberInitializedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("MemberInitializedEvent received");
                memberInitializedEventPublished = true;
            }
        });

        publishMemberInitializedEvent();

        while (!memberInitializedEventPublished) {
            log.info("Waiting until member initialized event is received...");
            sleep(1000);
        }
        assertTrue("Member initialized status check failed", StratosApiV41Utils.hasMembersInitialized(APPLICATION_ID));
    }

    private void publishMemberInitializedEvent() {
        MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(SERVICE_NAME, CLUSTER_ID,
                CLUSTER_INSTANCE_ID, TEST_MEMBER_ID_1, NETWORK_PARTITION_ID, PARTITION_ID, TEST_MEMBER_INSTANCE_ID_1);
        EventPublisher publisher = EventPublisherPool
                .getPublisher(MessagingUtil.getMessageTopicName(memberInitializedEvent));
        publisher.publish(memberInitializedEvent);
        log.info("MemberInitializedEvent published");
    }

    private void publishCompleteApplicationsEvent() {
        Map<String, ClusterDataHolder> clusterDataHolderMap = new HashMap<>();
        ClusterDataHolder clusterDataHolder = new ClusterDataHolder(SERVICE_NAME, CLUSTER_ID);
        clusterDataHolderMap.put("alias", clusterDataHolder);

        Application application = new Application(APPLICATION_ID);
        ApplicationInstance applicationInstance = new ApplicationInstance(APPLICATION_ALIAS, APPLICATION_INSTANCE_ID);
        application.addInstance("", applicationInstance);
        application.setClusterData(clusterDataHolderMap);

        Applications applications = new Applications();
        applications.addApplication(application);
        CompleteApplicationsEvent completeApplicationsEvent = new CompleteApplicationsEvent(applications);
        EventPublisher publisher = EventPublisherPool
                .getPublisher(MessagingUtil.getMessageTopicName(completeApplicationsEvent));
        publisher.publish(completeApplicationsEvent);
        log.info("CompleteApplicationsEvent published");
    }

    private void publishCompleteTopologyEvent() {
        Service service = new Service(SERVICE_NAME, ServiceType.SingleTenant);
        Cluster cluster = new Cluster(service.getServiceName(), CLUSTER_ID, DEPLOYMENT_POLICY_ID, AUTOSCALE_POLICY_ID,
                APPLICATION_ID);
        Member member = new Member(service.getServiceName(), cluster.getClusterId(), TEST_MEMBER_ID_1,
                CLUSTER_INSTANCE_ID, NETWORK_PARTITION_ID, PARTITION_ID, LoadBalancingIPType.Private,
                System.currentTimeMillis());
        member.setStatus(MemberStatus.Created);
        cluster.addMember(member);
        service.addCluster(cluster);
        Topology topology = new Topology();
        topology.addService(service);
        CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
        String topologyTopicName = MessagingUtil.getMessageTopicName(completeTopologyEvent);
        EventPublisher publisher = EventPublisherPool.getPublisher(topologyTopicName);
        publisher.publish(completeTopologyEvent);
        log.info("CompleteTopologyEvent published");
    }
}
