package org.apache.stratos.gce.extension;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.jclouds.util.Strings2;

import java.io.FileInputStream;
import java.io.IOException;

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
public class GCEOperations {

    private static final Log log = LogFactory.getLog(GCELoadBalancer.class);

    public GCEOperations() throws LoadBalancerExtensionException {

        //TODO: remove hard coded values
        String provider = "google-compute-engine";
        String identity = "969955727877-3q53n9vgjajebj9g7tigdosekedfviat@developer.gserviceaccount.com";
        String credential = "/home/sanjaya/key.pem";
        String groupName = "instance-group-1";
        credential = getPrivateKeyFromFile(credential);

        ComputeService compute = initComputeService(provider, identity, credential);


    }

    private static ComputeService initComputeService(String provider, String identity, String credential) {

        //initialize compute service

        Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

        ContextBuilder builder = ContextBuilder.newBuilder(provider)
                .credentials(identity, credential)
                .modules(modules);

        log.info("initializing " +  builder.getApiMetadata());

        return builder.buildView(ComputeServiceContext.class).getComputeService();
    }

    private static String getPrivateKeyFromFile(String filename) throws LoadBalancerExtensionException {
        try {
            return Strings2.toStringAndClose(new FileInputStream(filename));
        } catch (IOException e) {
            log.error("Exception : " + e);
            throw new LoadBalancerExtensionException(e);
        }
    }

}