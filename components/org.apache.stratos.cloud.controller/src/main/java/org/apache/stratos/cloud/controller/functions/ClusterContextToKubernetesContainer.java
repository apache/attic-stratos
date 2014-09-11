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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.ClusterContext;
import org.apache.stratos.cloud.controller.pojo.KubernetesClusterContext;
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.kubernetes.client.model.Container;
import org.apache.stratos.kubernetes.client.model.Port;
import com.google.common.base.Function;

/**
 *	Is responsible for converting a {@link ClusterContext} object to a Kubernetes {@link Container}
 *	Object.
 */
public class ClusterContextToKubernetesContainer implements Function<ClusterContext, Container>{
	
	private static final Log log = LogFactory.getLog(ClusterContextToKubernetesContainer.class);
	private FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

	@Override
	public Container apply(ClusterContext clusterContext) {
		Container container = new Container();
		container.setName(clusterContext.getHostName());

		Cartridge cartridge = dataHolder.getCartridge(clusterContext
				.getCartridgeType());
		
		if (cartridge == null) {
			log.error("Cannot find a Cartridge of type : "
					+ clusterContext.getCartridgeType());
			return null;
		}
		
		container.setImage(cartridge.getContainer().getImageName());

		container.setPorts(getPorts(clusterContext, cartridge));

		return container;
	}
	
	private Port[] getPorts(ClusterContext ctxt, Cartridge cartridge) {
		String kubernetesClusterId = CloudControllerUtil.getProperty(ctxt.getProperties(), 
        		StratosConstants.KUBERNETES_CLUSTER_ID);
		KubernetesClusterContext kubClusterContext = dataHolder.getKubernetesClusterContext(kubernetesClusterId);
		Port[] ports = new Port[cartridge.getPortMappings().size()];
		List<Port> portList = new ArrayList<Port>();
		
		for (PortMapping portMapping : cartridge.getPortMappings()) {
			Port p = new Port();
			p.setContainerPort(Integer.parseInt(portMapping.getPort()));
			p.setHostPort(kubClusterContext.getAnAvailableHostPort());
			p.setProtocol(portMapping.getProtocol());
			p.setName(p.getProtocol()+p.getContainerPort());
			portList.add(p);
		}
		
		return portList.toArray(ports);
	}

}
