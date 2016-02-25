source openrc admin admin

neutron net-create drexternal --provider:network_type flat --provider:physical_network dr-external --router:external
neutron subnet-create --name extsubnet --allocation-pool start=192.168.111.50,end=192.168.111.55  --gateway 192.168.111.253 --disable-dhcp drexternal 192.168.111.0/24
neutron router-gateway-set r1 drexternal
neutron floatingip-create drexternal
neutron port-list
neutron floatingip-list

echo "neutron floatingip-associate <floatingIPID> <PORTID>

neutron floatingip-associate"

