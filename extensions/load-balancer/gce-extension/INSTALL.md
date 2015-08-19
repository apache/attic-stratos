 #
 # Licensed to the Apache Software Foundation (ASF) under one
 # or more contributor license agreements. See the NOTICE file
 # distributed with this work for additional information
 # regarding copyright ownership. The ASF licenses this file
 # to you under the Apache License, Version 2.0 (the
 # "License"); you may not use this file except in compliance
 # with the License. You may obtain a copy of the License at
 #
 # http://www.apache.org/licenses/LICENSE-2.0
 #
 # Unless required by applicable law or agreed to in writing,
 # software distributed under the License is distributed on an
 # "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 # KIND, either express or implied. See the License for the
 # specific language governing permissions and limitations
 # under the License.
 #

# Installing Apache Stratos GCE Extension

Apache Stratos GCE Extension could be used for integrating GCE load balancer with Apache Stratos. Please follow
below steps to proceed with a quick installation:

1. Extract org.apache.stratos.gce.extension-<version>.zip to a desired location: <gce-extension-home>.

2. Open <gce-extension-home>/conf/gce-configuration file in text editor and update GCE credentials.

3. Open <gce-extension-home>/conf/jndi.properties file in a text editor and update message broker information:
   ```
   java.naming.provider.url=tcp://localhost:61616
   ```
4. Run <gce-extension-home>/bin/gce-extension.sh as the root user.

For a detailed installation refer following link:
https://docs.google.com/document/d/1a2ZptPScpjuavfpxVu1R1GC7R95jjzHo3L372zL2bRY/edit
