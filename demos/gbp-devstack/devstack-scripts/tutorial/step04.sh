source openrc admin admin

neutron net-create net3
neutron subnet-create net3 30.1.1.0/24 --name sub3 --gateway 30.1.1.1
neutron net-create net4
neutron subnet-create net4 40.1.1.0/24 --name sub4 --gateway 40.1.1.1

neutron router-create r2
neutron router-interface-add r2 sub3
neutron router-interface-add r2 sub4

novaboot-compute.sh net3 client_sg 3
novaboot-compute.sh net4 web_sg 3 

echo "control:"
nova list --host devstack-control
echo "compute:"
nova list --host devstack-compute-1

