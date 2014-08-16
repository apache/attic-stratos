#!/bin/bash
# --------------------------------------------------------------
#
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

# init file to run chef-client
# ---------------------------


MKDIR=`which mkdir`
UNZIP=`which unzip`
ECHO=`which echo`
FIND=`which find`
GREP=`which grep`
RM=`which rm`
XARGS=`which xargs`
SED=`which sed`
CUT=`which cut`
AWK=`which awk`
IFCONFIG=`which ifconfig`
HOSTNAME=`which hostname`
SLEEP=`which sleep`
TR=`which tr`
HEAD=`which head`
WGET=`which wget`

IP=`${IFCONFIG} eth0 | ${GREP} -e "inet addr" | ${AWK} '{print $2}' | ${CUT} -d ':' -f 2`
LOG=/tmp/puppet-init.log

HOSTSFILE=/etc/hosts
HOSTNAMEFILE=/etc/hostname
PUPPETCONF=/etc/puppet/puppet.conf
PUPPET=true

is_public_ip_assigned() {

while true
do
   wget http://169.254.169.254/latest/meta-data/public-ipv4
   if [ ! -f public-ipv4 ]
    	then
      	echo "Public ipv4 file not found. Sleep and retry" >> $LOG
      	sleep 2;
      	continue;
    	else
      	echo "public-ipv4 file is available. Read value" >> $LOG
      	# Here means file is available. Read the file
      	read -r ip<public-ipv4;
      	echo "value is **[$ip]** " >> $LOG

      	if [ -z "$ip" ]
        	then
          	echo "File is empty. Retry...." >> $LOG
          	sleep 2
          	rm public-ipv4
          	continue
         	else
           	echo "public ip is assigned. value is [$ip]. Remove file" >> $LOG
           	rm public-ipv4
           	break
         	fi
    	fi
done
}

run_puppet_agent() {
    ${ECHO} "Running puppet agent"

    PUPPET=`which puppet`
    PUPPETAGENT="${PUPPET} agent"
    RUNPUPPET="${PUPPETAGENT} -vt"

    ${SLEEP} 5
    ${PUPPETAGENT} --enable
    ${RUNPUPPET}
    ${PUPPETAGENT} --disable
}

run_chef_client() {
    ${ECHO} "Configuring chef-client"

    CHEF_CLIENT=`which chef-client`
    VALIDATER_KEY="/etc/chef/chef-validator.pem"

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
    printf '{"run_list" : ["role[%s]"]}\n' "${SERVICE_NAME}" > ${RUN_LIST_FILE}

    ${ECHO} "Running chef-client"
    CHEF_CLIENT_RUN="${CHEF_CLIENT} -j ${RUN_LIST_FILE}"
    ${CHEF_CLIENT_RUN}
}

set_hostnames() {
    local ipaddress=$1
    local hostname=$2

    ${ECHO} "${ipaddress}  ${hostname}" >> ${HOSTSFILE}
    ${ECHO} "127.0.0.1 ${HOST}" >> ${HOSTSFILE}

    /etc/init.d/hostname start
}

DATE=`date +%d%m%y%S`
RANDOMNUMBER="`${TR} -c -d 0-9 < /dev/urandom | ${HEAD} -c 4`${DATE}"

if [ ! -d /tmp/payload ]; then

	## Check whether the public ip is assigned
	is_public_ip_assigned

	echo "Public ip have assigned. Continue.." >> $LOG

	## Clean old poop
	${ECHO} "Removing all existing certificates .."
	#${FIND} /var/lib/puppet -type f -print0 | ${XARGS} -0r ${RM}

	${MKDIR} -p /tmp/payload
	${WGET} http://169.254.169.254/latest/user-data -O /tmp/payload/launch-params

	cd /tmp/payload
	SERVICE_NAME=`sed 's/,/\n/g' launch-params | grep SERVICE_NAME | cut -d "=" -f 2`
	DEPLOYMENT=`sed 's/,/\n/g' launch-params | grep DEPLOYMENT | cut -d "=" -f 2`
	INSTANCE_HOSTNAME=`sed 's/,/\n/g' launch-params | grep HOSTNAME | cut -d "=" -f 2`
	CONFIG_AUTO_FLAG=`sed 's/,/\n/g' launch-params | grep CONFIG_AUTO_FLAG | cut -d "=" -f 2`

	if [[ ${CONFIG_AUTO_FLAG} -eq "puppet" ]]; then
        PUPPET_IP=`sed 's/,/\n/g' launch-params | grep PUPPET_IP | cut -d "=" -f 2`
        PUPPET_HOSTNAME=`sed 's/,/\n/g' launch-params | grep PUPPET_HOSTNAME | cut -d "=" -f 2`
        PUPPET_ENV=`sed 's/,/\n/g' launch-params | grep PUPPET_ENV | cut -d "=" -f 2`

        #essential to have PUPPET_HOSTNAME at the end in order to auto-sign the certs
	    DOMAIN="${PUPPET_HOSTNAME}"
	elif [[ ${CONFIG_AUTO_FLAG} -eq "chef" ]]; then
	    PUPPET=false
	    CHEF_IP=`sed 's/,/\n/g' launch-params | grep CHEF_IP | cut -d "=" -f 2`
        CHEF_HOSTNAME=`sed 's/,/\n/g' launch-params | grep CHEF_HOSTNAME | cut -d "=" -f 2`

        # Assigning Chef server domain name for consistansy
        DOMAIN="${CHEF_HOSTNAME}"
	fi

	NODEID="${RANDOMNUMBER}.${DEPLOYMENT}.${SERVICE_NAME}"

	${ECHO} -e "\nNode Id ${NODEID}\n"
	${ECHO} -e "\nDomain ${DOMAIN}\n"

	HOST="${NODEID}.${DOMAIN}"
	${HOSTNAME} ${HOST}
	${ECHO} "${HOST}" > ${HOSTNAMEFILE}

	if [ ${PUPPET} = true ]; then
	    sed -i "s/server=.*/server=${PUPPET_HOSTNAME}/g"  ${PUPPETCONF}
	    /etc/init.d/puppet restart
	    ARGS=("-n${NODEID}" "-d${DOMAIN}" "-s${PUPPET_IP}")
	    #${ECHO} "${PUPPET_IP}  ${PUPPET_HOSTNAME}" >> ${HOSTSFILE}
	    set_hostnames ${PUPPET_IP} ${PUPPET_HOSTNAME}

	    run_puppet_agent
	else
	    #${ECHO} "${CHEF_IP}  ${CHEF_HOSTNAME}" >> ${HOSTSFILE}
	    set_hostnames ${CHEF_IP} ${CHEF_HOSTNAME}

	    run_chef_client
	fi

    ${ECHO} -e "Initialization completed successfully."

fi

# END

