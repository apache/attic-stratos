packages = ['nano','curl','wget','zip','unzip','tar']

packages.each do | pkg |
	action :upgrade
	log "Installed package : #{pkg}"
end
