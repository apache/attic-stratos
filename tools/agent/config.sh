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
# 	http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# --------------------------------------------------------------
shopt -s nocasematch
ECHO=`which echo`
RM=`which rm`
READ=`which read`
TR=`which tr`
HEAD=`which head`
WGET=`which wget`
MKDIR=`which mkdir`
GREP=`which grep`
SED=`which sed`
CP=`which cp`
MV=`which mv`
CURL=`which curl`

HOSTSFILE=/etc/hosts
DATE=`date +%d%m%y%S`
RANDOMNUMBER="`${TR} -c -d 0-9 < /dev/urandom | ${HEAD} -c 4`${DATE}"

function valid_ip()
{
    local  ip=$1
    local  stat=1

    if [[ $ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
        OIFS=$IFS
        IFS='.'
        ip=($ip)
        IFS=$OIFS
        [[ ${ip[0]} -le 255 && ${ip[1]} -le 255 \
            && ${ip[2]} -le 255 && ${ip[3]} -le 255 ]]
        stat=$?
    fi
    return $stat
}

set_hostnames() {
    local ipaddress=$1
    local hostname=$2

    ${ECHO} "${ipaddress}  ${hostname}" >> ${HOSTSFILE}
    ${ECHO} "127.0.0.1 ${HOST}" >> ${HOSTSFILE}

    /etc/init.d/hostname start
}

run_chef_client() {
    ${ECHO} "Configuring chef-client"

    CHEF_CLIENT=`which chef-client`
    #validater key should be copied by now
    VALIDATER_KEY="/etc/chef/chef-validator.pem"

    chmod 600 /etc/chef/*.pem

    ${ECHO} "Registering chef-client with server"
    CHEF_REGISTRATION="${CHEF_CLIENT} -S https://${CHEF_HOSTNAME} -K ${VALIDATER_KEY}"
    ${CHEF_REGISTRATION}

    ${ECHO} "Creating chef-client configuration"
    cat > /etc/chef/client.rb << EOF
    log_level       :info
    log_location    STDOUT
    chef_server_url "https://${CHEF_HOSTNAME}"
    EOF

    ${ECHO} "Creating run list"
    RUN_LIST_FILE=/etc/chef/run_list.json
    ${RM} -rf ${RUN_LIST_FILE}
    printf '{"run_list" : ["role[%s]"]}\n' "${SERVICE_NAME}" > ${RUN_LIST_FILE}

    ${ECHO} "Running chef-client"
    CHEF_CLIENT_RUN="${CHEF_CLIENT} -j ${RUN_LIST_FILE}"
    ${CHEF_CLIENT_RUN}
}

read -p "This script will install and configure puppet agent/chef client, do you want to continue [y/n]" answer
if [[ $answer = y ]] ; then

	${CP} -f ${HOSTSFILE} /etc/hosts.tmp
	${MKDIR} -p /tmp/payload
	${WGET} http://169.254.169.254/latest/user-data -O /tmp/payload/launch-params

	read -p "Please provide stratos service-name:" SERVICE_NAME
	if [[ -z $SERVICE_NAME ]]; then
	echo "service is empty!. Base image will be created."
        SERVICE_NAME=default
	fi

	read -p "Enter your configuration automation management choice. Currently Chef anf Puppet are supported. Use \"chef\" for Chef and \"puppet\" for Puppet. Default is Puppet. : " CONFIG_AUTO_FLAG
	CONFIG_AUTO_FLAG=${CONFIG_AUTO_FLAG:-puppet}

	if [[ ${CONFIG_AUTO_FLAG} -eq "chef" ]]; then
        read -p "Please provide Chef server IP:" CHEF_IP
        if ! valid_ip ${CHEF_IP} ; then
            echo "invalid IP address format!"
            exit -1
        fi

        read -p "Please provide Chef server hostname:" CHEF_HOSTNAME

        #read -p "Please provide stratos deployment:" DEPLOYMENT
        #DEPLOYMENT=${DEPLOYMENT:-default}
        DEPLOYMENT="default"

        NODEID="${RANDOMNUMBER}.${DEPLOYMENT}.${SERVICE_NAME}"

        ${ECHO} -e "\nNode Id ${NODEID}\n"
        ${ECHO} -e "\nDomain ${DOMAIN}\n"

        # set host names and update hosts file
        ${ECHO} -e "Updating hostnames"

        HOST="${NODEID}.${CHEF_HOSTNAME}"
        ${HOSTNAME} ${HOST}
        ${ECHO} ${HOST} >/etc/hostname

        set_hostnames ${CHEF_IP} ${CHEF_HOSTNAME}

	    #install_chef_client
	    ${ECHO} -e "Installing chef-client"
	    ${CURL} -L https://www.opscode.com/chef/install.sh | bash
	    ${MKDIR} /etc/chef
	    cd /etc/chef/

	    run_chef_client
	elif [[ ${CONFIG_AUTO_FLAG} -eq "puppet" ]]; then
        read -p "Please provide puppet master IP:" PUPPET_IP
        if ! valid_ip $PUPPET_IP ; then
        echo "invalid IP address format!"
        exit -1
        fi

        read -p "Please provide puppet master hostname [puppet.stratos.org]:" DOMAIN
        DOMAIN=${DOMAIN:-puppet.stratos.org}
        #essential to have PUPPET_HOSTNAME at the end in order to auto-sign the certs

        #read -p "Please provide stratos deployment:" DEPLOYMENT
        #DEPLOYMENT=${DEPLOYMENT:-default}
        DEPLOYMENT="default"

        NODEID="${RANDOMNUMBER}.${DEPLOYMENT}.${SERVICE_NAME}"

        ${ECHO} -e "\nNode Id ${NODEID}\n"
        ${ECHO} -e "\nDomain ${DOMAIN}\n"

        ARGS=("-n${NODEID}" "-d${DOMAIN}" "-s${PUPPET_IP}")
        ${ECHO} "\nRunning puppet installation with arguments: ${ARGS[@]}"
        /root/bin/puppetinstall/puppetinstall "${ARGS[@]}"
     else
        echo "Invalid selection for configuration automation!"
        exit -1
     fi

    #finally
    ${RM} /mnt/apache-stratos-cartridge-agent-4.0.0-SNAPSHOT/wso2carbon.lck
    ${GREP} -q '/root/bin/init.sh > /tmp/puppet_log' /etc/rc.local || ${SED} -i 's/exit 0$/\/root\/bin\/init.sh \> \/tmp\/puppet_log\nexit 0/' /etc/rc.local
    ${RM} -rf /tmp/*
    ${RM} -rf /var/lib/puppet/ssl/*
    ${MV} -f /etc/hosts.tmp ${HOSTSFILE}

fi

# END
