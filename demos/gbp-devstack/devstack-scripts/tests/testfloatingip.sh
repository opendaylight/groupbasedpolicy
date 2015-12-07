source openrc admin admin
sudo ovs-vsctl show
neutron security-group-create client_sg
neutron security-group-rule-create client_sg --direction egress --ethertype IPv4
neutron security-group-rule-create client_sg --direction ingress --ethertype IPv4

neutron net-create net1
neutron subnet-create net1 10.1.1.0/24 --name sub1 --gateway 10.1.1.1
neutron router-create router1
neutron router-interface-add router1 sub1
neutron net-create drexternal --provider:network_type flat --provider:physical_network dr-external --router:external
neutron subnet-create --name extsubnet --allocation-pool start=192.168.111.50,end=192.168.111.55  --gateway 192.168.111.100 --disable-dhcp drexternal 192.168.111.0/24
neutron router-gateway-set router1 drexternal
neutron floatingip-create drexternal
neutron floatingip-create drexternal
testnovaboot-control.sh net1 client_sg 1
testnovaboot-compute.sh net1 client_sg 2
neutron port-list
neutron floatingip-list

echo "neutron floatingip-associate <floatingIPID> <PORTID>

neutron floatingip-associate"

