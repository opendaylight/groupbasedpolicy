source openrc $1 $1 ; export OS_PASSWORD=$1

SG=$1"_client_sg"
NET1=$1"_net1"
NET2=$1"_net2"
SUB1=$1"_sub1"
SUB2=$1"_sub2"
R1=$1"_r1"


neutron security-group-create $SG 
neutron security-group-rule-create $SG --direction egress --ethertype IPv4
neutron security-group-rule-create $SG --direction ingress --ethertype IPv4

neutron net-create $NET1 

SUBNET1="10.1.2.0/24"
GW1="10.1.2.1"

if [ $1 = "coke" ]; then
    SUBNET1="10.1.1.0/24"
    GW1="10.1.1.1"
fi

neutron subnet-create $NET1 $SUBNET1 --name $SUB1 --gateway $GW1 
#neutron net-create $NET2
#neutron subnet-create $NET2 20.1.1.0/24 --name $SUB2 --gateway 20.1.1.1

#neutron router-create $R1
#neutron router-interface-add $R1 $SUB1
#neutron router-interface-add $R1 $SUB2


testnovaboot-control.sh $NET1 $SG 1  
#testnovaboot-compute.sh $NET1 $SG 2

echo "control:"
nova list --host devstack-control
echo "compute:"
nova list --host devstack-compute-1

