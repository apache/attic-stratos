package org.apache.stratos.gce.extension;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    //TODO: remove hardcoded values
    private static final String PROJECT_NAME = "GCE-test";
    private static final String projectId = "oceanic-depth-89120";
    private static final String zoneName = "europe-west1-d";
    private static final String regionName = "europe-west1";

    //auth
    private static final String keyFile = "/home/sanjaya/keys/p12key-donwloaded.p12";
    private static final String accountId = "164588286821-a517i85433f83e0nthc4qjmoupri" +
            "394q@developer.gserviceaccount.com";

    /**
     * Directory to store user credentials.
     */
    private static final String DATA_STORE_DIR = ".store/gce-extension";

    /**
     * Global instance of the JSON factory.
     */

    /**
     * OAuth 2.0 scopes
     */
    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE_READONLY);

    Compute compute;


    public GCEOperations() throws LoadBalancerExtensionException, GeneralSecurityException, IOException {

        buildComputeEngineObject();

    }

    private void createTargetPool(String targetPoolName) {


    }

    private void buildComputeEngineObject() throws GeneralSecurityException, IOException {

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        DataStoreFactory dataStoreFactory = new FileDataStoreFactory(new
                File(System.getProperty("user.home"), DATA_STORE_DIR));
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        GoogleCredential credential = new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(accountId)
                .setServiceAccountScopes(Collections.singleton(ComputeScopes.COMPUTE))
                .setServiceAccountPrivateKeyFromP12File(new File(keyFile))
                .build();

        // Create compute engine object for listing instances
        compute = new Compute.Builder(
                httpTransport, jsonFactory, null).setApplicationName(PROJECT_NAME)
                .setHttpRequestInitializer(credential).build();


    }


}