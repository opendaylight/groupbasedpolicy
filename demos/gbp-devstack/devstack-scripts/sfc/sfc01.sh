#!/usr/bin/env bash

if [ -f "/home/vagrant/sfc01.lock" ]; then
    echo "You have already run sfc01"
    exit
fi
echo "writing lock file /home/vagrant/sfc01.lock"
touch /home/vagrant/sfc01.lock

#echo "Setting GBP table offset to 20"
#sh curl --user admin:admin  --header "Content-Type:application/json"  -t PUT  --data '{ "of-overlay-config": { "gbp-ofoverlay-table-offset": "19" }}'  http://$ODL:8181/restconf/config/ofoverlay:of-overlay-config

echo "Importing image for SF"
/vagrant/devstack-scripts/importSF.sh

echo "Making aggregates/availability zones under admin/admin:"
source openrc admin admin

nova hypervisor-list
nova aggregate-create control az-control
nova aggregate-add-host 1 devstack-control
nova aggregate-create compute az-compute
nova aggregate-add-host 2 devstack-compute-1

#echo "Adding key pair cloudkey to admin and service tenants"
#nova keypair-add --pub-key cloud.key.pub cloudkey
#source openrc heat service
#nova keypair-add --pub-key cloud.key.pub cloudkey


echo "Making infrastructure (SecGrps, Networks, Router) for GBP managed workloads under admin/admin:"
source openrc admin admin

neutron security-group-create client_sg
neutron security-group-rule-create client_sg --direction ingress --ethertype IPv4
neutron security-group-rule-create client_sg --direction egress --ethertype IPv4

neutron security-group-create web_sg
neutron security-group-rule-create web_sg --direction ingress --ethertype IPv4
neutron security-group-rule-create web_sg --direction egress --ethertype IPv4

neutron net-create net1
neutron subnet-create net1 10.1.1.0/24 --name sub1 --gateway 10.1.1.1 --dns-nameservers list=true 8.8.4.4 8.8.8.8

neutron net-create net2
neutron subnet-create net2 20.1.1.0/24 --name sub2 --gateway 20.1.1.1 --dns-nameservers list=true 8.8.4.4 8.8.8.8

neutron router-create r1
neutron router-interface-add r1 sub1
neutron router-interface-add r1 sub2

novaboot-control.sh net1 client_sg 1
novaboot-control.sh net2 web_sg 2
novaboot-control.sh net1 client_sg 2


echo "control:"
nova list --host devstack-control
echo "compute:"
nova list --host devstack-compute-1


