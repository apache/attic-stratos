# require stratos_base

default[:base][:package_repo]          = "http://10.4.128.7"
default[:base][:local_package_dir]     = "/mnt/packs"

default[:mb][:ip]                      = "127.0.0.1"
default[:mb][:port]                    = "61616"
default[:mb][:type]                    = "activemq" #in wso2 mb case, value should be 'wso2mb'

default[:cep][:ip]                     = "127.0.0.1"
default[:cep][:port]                   = "7611"

default[:truststore][:password]        = 'wso2carbon'

default[:java][:distribution]          ='jdk-7u51-linux-x64.tar.gz'
default[:java][:name]                  ='jdk1.7.0_51'

default[:lb][:member_type_ip]          ='private'
default[:lb][:http_port]               = '80'
default[:lb][:https_port]              = '443'

default[:tomcat][:version]             = '7.0.52'

default[:agent][:enable_log_publisher] = 'false'

default[:bam][:ip]                     ='127.0.0.1'
default[:bam][:port]                   ='7611'
default[:bam][:secure_port]            ='7711'
default[:bam][:username]               = 'admin'
default[:bam][:password]               = 'admin'

#include_attribute "stratos_base" included in the recipe