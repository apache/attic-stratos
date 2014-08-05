# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Usage
# -----
#
#  class {'phppgadmin':
#    db_host	   => 'localhost',
#    db_port        => '5432',
#    owned_only 	   => false,
#    extra_login_security => false
#  }
#
# db_host : PostgreSQL server host which needs to be managed
# db_port : PostgreSQL server port
# ownded_only : Whether to disply own databases or all databases to a logged in user
# extra_login_security : Restrict remote login

class phppgadmin(
  $db_host  = $phppgadmin::params::db_host,
  $db_port  = $phppgadmin::params::db_port,
  $extra_login_security = $phppgadmin::params::extra_login_security,
  $owned_only = $phppgadmin::params::owned_only
) inherits phppgadmin::params {

  package{$phppgadmin::params::phppgadmin_package:
    ensure => installed,
  }

  file{$phppgadmin::params::http_conf_file:
    ensure  => present,
    mode    => '0644',
    content => template($phppgadmin::params::http_conf_template_file),
    require => Package[$phppgadmin::params::phppgadmin_package],
  }

  file{$phppgadmin::params::phppgadmin_conf_file:
    ensure  => present,
    mode    => '0644',
    content => template($phppgadmin::params::phppgadmin_conf_template_file),
    require => Package[$phppgadmin::params::phppgadmin_package],
  }

  file{$phppgadmin::params::default_host_file:
    ensure  => present,
    mode    => '0644',
    content => template($phppgadmin::params::default_host_template_file),
    require => Package[$phppgadmin::params::phppgadmin_package],
  }

  service { apache2:
    ensure => running,
    enable => true,
    subscribe =>[ 
		  File[$phppgadmin::params::http_conf_file],
 	          File[$phppgadmin::params::phppgadmin_conf_file],
		  File[$phppgadmin::params::default_host_file],
		],
  }
}

