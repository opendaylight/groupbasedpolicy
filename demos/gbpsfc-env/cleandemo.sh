#!/usr/bin/env bash


for i in `seq 1 $NUM_NODES`; do
  hostname="gbpsfc"$i
  switchname="sw"$i
  echo $hostname
  vagrant ssh $hostname -c "sudo ovs-vsctl del-br $switchname; sudo ovs-vsctl del-manager; sudo /vagrant/vmclean.sh; sudo /vagrant/sflow/stop_sflow-rt.sh  >/dev/null 2>&1"

done

./rest-clean.py

if [ -f "demo.lock" ] ; then
  rm demo.lock
fi
