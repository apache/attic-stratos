packages = ['nano','curl','wget','zip','unzip','tar']

packages.each do | pkg |
	package pkg do
		action :upgrade
	end

	log "Installed package : #{pkg}"
end
