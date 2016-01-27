source openrc admin admin
echo "Hypervisors:"
nova hypervisor-list
echo "Networks:"
neutron net-list
echo "Subnets: "
neutron subnet-list
echo "Ports: "
neutron port-list
echo "Security grous:"
neutron security-group-list
echo "Routers and router ports: "
neutron router-list
echo "Nova instances on compute1:"
nova list --host devstack-compute-1
echo "Nova instances on control:"
nova list --host devstack-control
