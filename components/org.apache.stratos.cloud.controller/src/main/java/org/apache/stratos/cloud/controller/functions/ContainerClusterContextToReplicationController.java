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
package org.apache.stratos.cloud.controller.functions;

import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.ClusterContext;
import org.apache.stratos.cloud.controller.domain.ContainerClusterContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.kubernetes.client.model.Container;
import org.apache.stratos.kubernetes.client.model.Label;
import org.apache.stratos.kubernetes.client.model.Manifest;
import org.apache.stratos.kubernetes.client.model.Pod;
import org.apache.stratos.kubernetes.client.model.ReplicationController;
import org.apache.stratos.kubernetes.client.model.Selector;
import org.apache.stratos.kubernetes.client.model.State;

import com.google.common.base.Function;

/**
 * Is responsible for converting a {@link ContainerClusterContext} object to a Kubernetes
 * {@link ReplicationController} Object.
 */
public class ContainerClusterContextToReplicationController implements
        Function<MemberContext, ReplicationController> {

    @Override
    public ReplicationController apply(MemberContext memberContext) {

        String clusterId = memberContext.getClusterId();
        ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);

        ReplicationController replicationController = new ReplicationController();
        replicationController.setId(clusterContext.getClusterId());
        replicationController.setKind("ReplicationController");
        replicationController.setApiVersion("v1beta1");
        State desiredState = new State();
        String minReplicas = CloudControllerUtil.getProperty(clusterContext.getProperties(),
                StratosConstants.KUBERNETES_MIN_REPLICAS);
        desiredState.setReplicas(Integer.parseInt(minReplicas));
        Selector selector = new Selector();
        selector.setName(clusterContext.getClusterId());
        desiredState.setReplicaSelector(selector);

        Pod podTemplate = new Pod();
        State podState = new State();
        Manifest manifest = new Manifest();
        manifest.setVersion("v1beta1");
        manifest.setId(clusterContext.getClusterId());

        ContainerClusterContextToKubernetesContainer containerFunc = new ContainerClusterContextToKubernetesContainer();
        Container container = containerFunc.apply(memberContext);

        manifest.setContainers(new Container[] { container });

        podState.setManifest(manifest);
        podTemplate.setDesiredState(podState);
        Label l1 = new Label();
        l1.setName(clusterContext.getClusterId());
        podTemplate.setLabels(l1);

        desiredState.setPodTemplate(podTemplate);
        replicationController.setDesiredState(desiredState);

        Label l2 = new Label();
        l2.setName(clusterContext.getClusterId());
        replicationController.setLabels(l2);

        return replicationController;
    }

}
