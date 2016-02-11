#!/usr/bin/env bash
if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters
    Arg1= tap port name of SF
    Arg2= IP address of SF 
    Arg3= Mac Addr of SF"
    exit 1
fi

TAP=$1
IP=$2
MAC=$3
TDEST=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'`
sudo ip route add $2/32 dev $1 
sudo arp -i $1 -s $2 $3 

