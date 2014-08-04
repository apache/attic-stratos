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

class phppgadmin::params{

  $db_host  = 'localhost'
  $db_port  = '5432'
  $extra_login_security = false
  $owned_only = true

  case $::operatingsystem {
    'RedHat', 'CentOS', 'Fedora': {
      $phppgadmin_package = 'phpPgAdmin'
      $phppgadmin_conf_file = '/etc/phpPgAdmin/config.inc.php'
      $phppgadmin_conf_template_file = 'phppgadmin/CentOS/config.inc.php.erb'
      $http_conf_file     = '/etc/httpd/conf.d/phpPgAdmin.conf'
      $http_conf_template_file = 'phppgadmin/CentOS/phpPgAdmin.conf.erb'
      $default_host_file     = '/etc/apache2/sites-available/default'
      $default_host_template_file     = 'phppgadmin/CentOS/default.erb'
    }
    'Debian', 'Ubuntu': {
      $phppgadmin_package = 'phppgadmin'
      $phppgadmin_conf_file = '/etc/phppgadmin/config.inc.php'
      $phppgadmin_conf_template_file = 'phppgadmin/Ubuntu/config.inc.php.erb'
      $http_conf_file     = '/etc/phppgadmin/apache.conf'
      $http_conf_template_file = 'phppgadmin/Ubuntu/phppgadmin.conf.erb'
      $default_host_file     = '/etc/apache2/sites-available/default'
      $default_host_template_file     = 'phppgadmin/Ubuntu/default.erb'
    }
    default: {
      fail("Unsupported platform: ${::osfamily}|${::operatingsystem}")
    }
  }
}
