neutron security-group-create client_sg
neutron security-group-rule-create client_sg --direction egress --protocol tcp --port-range-min 80 --port-range-max 80
neutron security-group-rule-create client_sg --direction egress --protocol tcp --port-range-min 443 --port-range-max 443
neutron security-group-rule-create client_sg --direction egress --protocol tcp --port-range-min 5004 --port-range-max 5004
neutron security-group-rule-create client_sg --direction egress --protocol udp --port-range-min 5004 --port-range-max 5004
neutron security-group-rule-create client_sg --direction ingress --ethertype IPv4

neutron security-group-create video_sg
neutron security-group-rule-create video_sg --direction ingress --protocol tcp --port-range-min 80 --port-range-max 80
neutron security-group-rule-create video_sg --direction ingress --protocol tcp --port-range-min 443 --port-range-max 443
neutron security-group-rule-create video_sg --direction ingress --protocol tcp --port-range-min 5004 --port-range-max 5004
neutron security-group-rule-create video_sg --direction ingress --protocol udp --port-range-min 5004 --port-range-max 5004
neutron security-group-rule-create video_sg --direction egress --ethertype IPv4

neutron security-group-create game_sg
neutron security-group-rule-create game_sg --direction egress --ethertype IPv4
neutron security-group-rule-create game_sg --direction ingress --protocol tcp --port-range-min 443 --port-range-max 443

neutron net-create net1
neutron subnet-create net1 10.1.1.0/24 --name sub1 --gateway 10.1.1.1
neutron net-create net2
neutron subnet-create net2 20.1.1.0/24 --name sub2 --gateway 20.1.1.1
neutron net-create net3
neutron subnet-create net3 30.1.1.0/24 --name sub3 --gateway 30.1.1.1
neutron net-create net4
neutron subnet-create net4 40.1.1.0/24 --name sub4 --gateway 40.1.1.1
neutron net-create net5
neutron subnet-create net5 50.1.1.0/24 --name sub5 --gateway 50.1.1.1

neutron router-create r1
neutron router-interface-add r1 sub1
neutron router-interface-add r1 sub2

novaboot.sh net1 client_sg 1 #"bekind" 
novaboot.sh net1 video_sg 1 
novaboot.sh net1 game_sg 1 

novaboot.sh net2 client_sg 2 
novaboot.sh net2 video_sg 2 
novaboot.sh net2 game_sg 2 #"bekind"

neutron router-create r2
neutron router-interface-add r2 sub3
neutron router-interface-add r2 sub4
neutron router-interface-add r2 sub5

novaboot.sh net3 client_sg 3 
novaboot.sh net3 client_sg 4

novaboot.sh net4 video_sg 3
novaboot.sh net4 game_sg 3

novaboot.sh net5 video_sg 4 
novaboot.sh net5 game_sg 4 

echo "control:"
nova list --host devstack-control
echo "compute:"
nova list --host devstack-compute-1

