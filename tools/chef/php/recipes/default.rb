#--------------------------------------------------------------
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
#--------------------------------------------------------------


packages = %w(
    'build-essential'
    'mysql-client'
    'apache2'
    'php5'
    'php5-cli'
    'libapache2-mod-php5'
    'php5-gd'
    'php5-mysql'
    'php-db'
    'php-pear'
    'php5-curl'
    'php5-ldap'
    'php5-adodb'
    'mailutils'
    'php5-imap'
    'php5-sqlite'
    'php5-xmlrpc'
    'php5-xsl'
    'openssl'
    'ssl-cert'
    'ldap-utils'
    'php5-mcrypt'
    'mcrypt'
    'ufw'
    'fail2ban'
    'git'
    'libboost-all-dev'
    'ruby')

cookbook_file "/etc/apt/apt.conf.d/90forceyes" do
  mode 0755
end

if FileTest.exists?("/etc/apt/apt.conf.d/90forceyes")
  execute "apt-update" do
    command "apt-get update > /dev/null 2>&1"
  end
  log "Running apt update"
end

#Installing packages
packages.each do |pkg|
  package pkg
  log "Installed package : #{pkg}"
end


#Creating apache configuration file
template "/etc/apache2/apache2.conf" do
  source "apache2/apache2.conf.erb"
  owner 'root'
  group 'root'
  mode '0775'
  action :create
end

#Creating defaulf host file
if FileTest.exists?("/etc/apache2/apache2.conf")
  template "/etc/apache2/sites-available/default" do
    source "apache2/sites-available/default.erb"
    owner 'root'
    group 'root'
    mode '0775'
    action :create
  end
end

#Creating default ssl host file
if FileTest.exists?("/etc/apache2/sites-available/default")
  template "/etc/apache2/sites-available/default-ssl" do
    source "apache2/sites-available/default-ssl.erb"
    owner 'root'
    group 'root'
    mode '0775'
    action :create
  end
end

#Enabling ssl module
if FileTest.exists?("/etc/apache2/sites-available/default-ssl")
  execute "enable ssl module" do
    command "a2enmod ssl"
    action :run
  end
  log "Enabled ssl module"
end

#Restarting apache to apply cheges
if FileTest.exists?("/etc/apache2/apache2.conf")
  execute "apache2 relaod" do
    command "service apache2 restart"
    action :run
  end
  log "Apache2 reloaded"
end

