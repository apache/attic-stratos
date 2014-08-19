#tomcat config

package_name="apache-tomcat-#{node[:tomcat][:version]}"
service_code="apache-tomcat"
tomcat_home="#{node[:tomcat][:target]}/#{package_name}"

cookbook_file "/#{node[:tomcat][:target]}/packs/apache-tomcat-#{node[:tomcat][:version]}.tar.gz" do
 	source "apache-tomcat-#{node[:tomcat][:version]}.tar.gz"
 end

execute 'extract tomcat package' do
	path 	"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
	cwd 	target
	not_if 	{ ::File.exists?("#{node[:tomcat][:target]}/#{tomcat_home}/conf")}
	command	"tar xvfz #{node[:tomcat][:target]}/packs/#{package_name}.tar.gz"
	#logoutput
	creates	"#{node[:tomcat][:target]}/#{tomcat_home}/conf"
	#require   => File["/${target}/packs/apache-tomcat-${tomcat_version}.tar.gz"];
end

template("#{tomcat_home}/conf/server.xml") do
  #require  => Exec['Extract tomcat package'];
  action :create
end

template "/mnt/tomcat"

execute "Set tomcat home permission" do
	path 	"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
    cwd 	target
    command "chown -R #{node[:tomcat][:owner]} #{tomcat_home}; chmod -R 755 #{tomcat_home}"
end

execute "Start tomcat" do
	path	"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
	cwd 	"#{tomcat_home}/bin"
	environment	({'JAVA_HOME' => '/opt/java'})
	command	"bash startup.sh"
	# logoutput   => 'on_failure',
end

