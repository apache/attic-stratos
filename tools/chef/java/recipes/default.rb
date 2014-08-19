java_home = "/opt/#{node[:java][:name]}"
package = node[:java][:distribution]
local_dir = node[:base][:local_package_dir]

cookbook_file "#{package}" do
	# copy jdk to files/
	path "/opt/#{package}"
	mode "0755"
	#ignore => '.svn';
end

execute "Install Java" do
	path "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
	cwd '/opt'
	command "/bin/tar xzf ${package}"
	not_if { ::File.exists?(java_home)}
	creates "${java_home}/COPYRIGHT"
	action :run
end

link "/opt/java" do
	to "#{java_home}"
end

template "/etc/profile.d/java_home.sh" do
	mode "0755"
end


