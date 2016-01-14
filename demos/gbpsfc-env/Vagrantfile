
# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  odl=ENV['ODL']
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "512"
  end
  # run our bootstrapping for the system
  num_nodes = (ENV['NUM_NODES'] || 1).to_i

  # ip configuration
  ip_base = (ENV['SUBNET'] || "192.168.50.")
  ips = num_nodes.times.collect { |n| ip_base + "#{n+70}" }

  ip_base_sflow = "192.168.53."
  ips_sflow = num_nodes.times.collect { |n| ip_base_sflow + "#{n+70}" }

  num_nodes.times do |n|
    config.vm.define "gbpsfc#{n+1}", autostart: true do |compute|
      vm_ip = ips[n]
      vm_ip_sflow = ips_sflow[n]
      vm_index = n+1
      compute.vm.box = "alagalah/gbpsfc-trusty64"
      compute.vm.box_version = "1.0.1"
      compute.vm.hostname = "gbpsfc#{vm_index}"
      compute.vm.network "private_network", ip: "#{vm_ip}"
      compute.vm.network "private_network", ip: "#{vm_ip_sflow}"
      compute.vm.provider :virtualbox do |vb|
        vb.memory = 512
        vb.customize ["modifyvm", :id, "--ioapic", "on"]      
        vb.cpus = 1
      end
    end
  end
end
