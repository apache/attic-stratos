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

package org.apache.stratos.rest.endpoint.bean.kubernetes;

import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PropertyBean;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class KubernetesGroup {

    private String groupId;
    private String description;
    private List<KubernetesHost> kubernetesHosts;
    private KubernetesMaster kubernetesMaster;
    private PortRange portRange;
    private List<PropertyBean> property;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<KubernetesHost> getKubernetesHosts() {
        return kubernetesHosts;
    }

    public void setKubernetesHosts(List<KubernetesHost> kubernetesHosts) {
        this.kubernetesHosts = kubernetesHosts;
    }

    public KubernetesMaster getKubernetesMaster() {
        return kubernetesMaster;
    }

    public void setKubernetesMaster(KubernetesMaster kubernetesMaster) {
        this.kubernetesMaster = kubernetesMaster;
    }

    public PortRange getPortRange() {
        return portRange;
    }

    public void setPortRange(PortRange portRange) {
        this.portRange = portRange;
    }

    public List<PropertyBean> getProperty() {
        return property;
    }

    public void setProperty(List<PropertyBean> property) {
        this.property = property;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
