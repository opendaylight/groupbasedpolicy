neutron security-group-create web_sg
neutron security-group-rule-create web_sg --direction ingress --protocol tcp --port-range-min 80 --port-range-max 80
neutron security-group-rule-create web_sg --direction egress --ethertype IPv4
neutron security-group-create secured_web_sg
neutron security-group-rule-create secured_web_sg --direction ingress --protocol tcp --port-range-min 443 --port-range-max 443
neutron security-group-rule-create secured_web_sg --direction egress --ethertype IPv4
neutron security-group-create client_sg
neutron security-group-rule-create client_sg --direction egress --protocol tcp --port-range-min 80 --port-range-max 80
neutron security-group-rule-create client_sg --direction egress --protocol tcp --port-range-min 443 --port-range-max 443
neutron security-group-rule-create client_sg --direction ingress --ethertype IPv4


neutron net-create net1
neutron subnet-create net1 10.1.1.0/24 --name sub1 --gateway 10.1.1.1
neutron net-create net2
neutron subnet-create net2 20.1.1.0/24 --name sub2 --gateway 20.1.1.1

novaboot.sh net1 client_sg
novaboot.sh net1 web_sg
novaboot.sh net1 secured_web_sg

