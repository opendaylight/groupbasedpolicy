NETWORK=$1
SEC_GRP=$2
VM=$3
IMAGE=$4
FLAVOR=$5

if [ $# -eq 0 ]
  then
    echo "Usage: novaboot.sh {network} {security-group-name} {vm#} {image}.
	ie. novaboot.sh net1 client_sg 3 lubuntu|cirros-0.3.2-x86_64-uec"
    exit
fi

: ${VM:=1}
: ${IMAGE:=cirros-0.3.4-x86_64-uec}


IMAGE_ID=`nova image-list | egrep $IMAGE | awk '{print $2}'`
set -- $IMAGE_ID
IMAGE_ID=$1
PORT_ID=`neutron port-create $NETWORK --security-group $SEC_GRP | egrep "\sid\s" | awk '{print $4}'`
#echo "image ",$IMAGE_ID
#echo "flavor ", $FLAVOR
#echo "port ",$PORT_ID
#echo "name ", $SECGRP, $VM
nova boot --image $IMAGE_ID --flavor $FLAVOR --nic port-id=$PORT_ID "$NETWORK-$SEC_GRP-$VM" 

