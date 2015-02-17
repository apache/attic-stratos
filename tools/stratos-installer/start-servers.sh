#!/bin/bash
# ----------------------------------------------------------------------------
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
# ----------------------------------------------------------------------------
#
# This script is for starting Apache Stratos servers.
# ----------------------------------------------------------------------------

# Die on any error:
set -e
product_list=$1
export LOG=$log_path/stratos.log
<<<<<<< HEAD

profile="default"
=======
SLEEP=100
>>>>>>> FETCH_HEAD

if [[ -f ./conf/setup.conf ]]; then
    source "./conf/setup.conf"
    echo "source it"
fi


function help {
    echo ""
    echo "Give the profile to start on this machine. The available profiles are"
    echo "cc, as, sm, default. 'default' means that a single product with all features will be started."
    echo "usage:"
    echo "start-servers.sh -p\"<profile>\""
    echo "eg."
    echo "start-servers.sh -p\"cc\""
    echo ""
}

while getopts p: opts
do
  case $opts in
    p)
        profile_list=${OPTARG}
        ;;
    \?)
        help
        exit 1
        ;;
  esac
done


arr=$(echo $profile_list | tr " " "\n")

for x in $arr
do
    if [[ $x = "cc" ]]; then
        profile="cc"
    elif [[ $x = "as" ]]; then
        profile="as"
    elif [[ $x = "sm" ]]; then
        profile="sm"
    else
        echo "'default' profile selected."
        profile="default"
    fi
done
<<<<<<< HEAD

stratos_extract_path=$stratos_extract_path"-"$profile

if [[ $profile = "default" ]]; then
    echo "Starting ActiveMQ server ..." >> $LOG
    $activemq_path/bin/activemq start
    echo "ActiveMQ server started" >> $LOG
    sleep 10
fi

echo "Starting wso2 greg server ..." >> $LOG
echo "$greg_extract_path/bin/wso2server.sh -Dprofile=$profile start"
$greg_extract_path/bin/wso2server.sh -Dprofile=$profile start
echo "Stratos server started" >> $LOG

echo "Starting Stratos server ..." >> $LOG
echo "$stratos_extract_path/bin/stratos.sh -Dprofile=$profile start"
$stratos_extract_path/bin/stratos.sh -Dprofile=$profile start
echo "Stratos server started" >> $LOG
=======
product_list=`echo $product_list | sed 's/^ *//g' | sed 's/ *$//g'`
if [[ -z $product_list || $product_list = "" ]]; then
    help
    exit 1
fi


if [[ $cc = "true" ]]; then
    echo ${cc_path}

    echo "Starting CC server ..." >> $LOG
    nohup ${cc_path}/bin/stratos.sh -DportOffset=$cc_port_offset &
    echo "CC server started" >> $LOG
    sleep $SLEEP
fi

if [[ $elb = "true" ]]; then
    echo ${elb_path} 

    echo "Starting ELB server ..." >> $LOG
    nohup ${elb_path}/bin/stratos.sh -DportOffset=$elb_port_offset &
    echo "ELB server started" >> $LOG
    sleep $SLEEP
fi

if [[ $agent = "true" ]]; then
    echo ${agent_path}

    echo "Starting AGENT server ..." >> $LOG
    nohup ${agent_path}/bin/stratos.sh -DportOffset=$agent_port_offset &
    echo "AGENT server started" >> $LOG
    sleep $SLEEP
fi

if [[ $sc = "true" ]]; then
    
    echo ${sc_path}

    echo "Starting SC server ..." >> $LOG
    nohup ${sc_path}/bin/stratos.sh -DportOffset=$sc_port_offset &
    echo "SC server started" >> $LOG
fi

>>>>>>> FETCH_HEAD
