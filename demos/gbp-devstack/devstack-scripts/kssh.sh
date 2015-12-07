#!/bin/sh
ip=$1
nnet=`neutron net-list`
nlist=`nova list`
uuid=`echo "$nnet" | grep $(echo "$nlist" | grep $ip | awk '{print $12}' | awk 'BEGIN {FS="="} {print $1}') | awk '{print $2}'`
cmd="ip netns exec qdhcp-$uuid rm -rf /root/.ssh/known_hosts"
sudo $cmd
cmd="ip netns exec qdhcp-$uuid ssh cirros@$ip"
sudo $cmd

