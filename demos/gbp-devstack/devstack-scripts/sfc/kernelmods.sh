#!/usr/bin/env bash

TAP=$1
IP=$2
MAC=$3
TDEST=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'`
sudo ip route add $2/32 dev $1 
sudo arp -i $1 -s $2 $3 

echo "Flow mod TBD"
sudo ovs-ofctl add-flow br-int "table=0,ip,nw_dst=$TDEST,actions=output:4" -OOpenFlow13
resetcontroller.sh
