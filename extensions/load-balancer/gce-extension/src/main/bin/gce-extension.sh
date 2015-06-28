#!/bin/bash
# --------------------------------------------------------------
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# --------------------------------------------------------------

echo "Starting gce load balancer extension..."
script_path="$( cd -P "$( dirname "$SOURCE" )" && pwd )/`dirname $0`"
echo ${script_path}
lib_path=${script_path}/../lib/
class_path=`echo ${lib_path}/*.jar | tr ' ' ':'`



properties="-Djndi.properties.dir=${script_path}/../conf
            -Dstats.socket.file.path=/tmp/haproxy-stats.socket
            -Dlog4j.properties.file.path=${script_path}/../conf/log4j.properties
            -Djavax.net.ssl.trustStore=${script_path}/../security/client-truststore.jks
            -Djavax.net.ssl.trustStorePassword=wso2carbon
            -Dthrift.client.config.file.path=${script_path}/../conf/thrift-client-config.xml
            -Dcep.stats.publisher.enabled=false
            -Dthrift.receiver.ip=127.0.0.1
            -Dthrift.receiver.port=7615
            -Dnetwork.partition.id=network-partition-1
            -Dcluster.id=cluster-1
            -Dservice.name=php
            -Dname.prefix=lb
            -Dproject.name=MyFirstProject
            -Dproject.id=gold-access-96509
            -Dzone.name=europe-west1-b
            -Dregion.name=europe-west1
            -Dkey.file.path=/home/sanjaya/keys/p12key-donwloaded.p12
            -Dgce.account.id=164588286821-a517i85433f83e0nthc4qjmoupri394q@developer.gserviceaccount.com
            -Dhealth.check.request.path=/
            -Dhealth.check.port=80
            -Dhealth.check.timeout.sec=5
            -Dhealth.check.unhealthy.threshold=2
            -Dnetwork.name=default
            -Doperation.timeout=10000"

# Uncomment below line to enable remote debugging
debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006"

java -cp "${class_path}" ${properties} ${debug} org.apache.stratos.gce.extension.Main $*