source openrc admin admin

neutron net-create net2
neutron subnet-create net2 20.1.1.0/24 --name sub2 --gateway 20.1.1.1 --dns-nameservers list=true 8.8.4.4 8.8.8.8

neutron router-create r1
neutron router-interface-add r1 sub1
neutron router-interface-add r1 sub2


novaboot-compute.sh net2 client_sg 2
novaboot-control.sh net2 web_sg 2

echo "control:"
nova list --host devstack-control
echo "compute:"
nova list --host devstack-compute-1

