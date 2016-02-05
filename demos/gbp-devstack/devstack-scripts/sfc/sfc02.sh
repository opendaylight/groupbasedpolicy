#!/usr/bin/env bash

if [ -f "/home/vagrant/sfc02.lock" ]; then
    echo "You have already run sfc02"
    exit
fi
echo "writing lock file /home/vagrant/sfc02.lock"
touch /home/vagrant/sfc02.lock

echo "Making OOB management network and security groups for SF in heat/service:"
source openrc heat service
#neutron net-create sf_mgmt
#neutron subnet-create sf_mgmt 30.1.1.0/24 --name sf_mgmt_sub --gateway 30.1.1.1 

#neutron security-group-create sf_mgmt
#neutron security-group-rule-create sf_mgmt --direction ingress --ethertype IPv4
#neutron security-group-rule-create sf_mgmt --direction egress --ethertype IPv4

echo "Making SFC net_mgmt for inband SFC traffic:"
neutron net-create net_mgmt #--provider:network_type=flat --provider:physical_network dr-external --router:external
neutron subnet-create net_mgmt 11.0.0.0/24

echo "Import VNFD for test-VNF:"
tacker vnfd-create --vnfd-file /vagrant/devstack-scripts/sfc-random/test-vnfd.yaml

echo "Deploy VNFs:"
tacker vnf-create --name testVNF1 --vnfd-name test-vnfd
#tacker vnf-create --name testVNF2 --vnfd-name test-vnfd




echo "Wait a few minutes and then check VNF status is ACTIVE (tacker vnf-list) then execute the following commands on devstack-compute-1 BEFORE running sfc03.sh:

sudo ovs-vsctl show
sudo ovs-vsctl del-port vxlangpe-br-int
sudo ovs-vsctl del-port vxlan-br-int
sudo ovs-vsctl show

- remove VNF tenant from DataStore, it borks PolEnf"

