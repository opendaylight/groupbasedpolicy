#!/usr/bin/env bash

set -e

demo=${1%/}

echo $demo

if [ -f "demo.lock" ]; then
    echo "There is already a demo running:"
    cat demo.lock
    exit
fi

cp $demo/infrastructure_config.py .

if [ -f $demo/sf-config.sh ]; then
    cp $demo/sf-config.sh .
fi

echo "Starting demo from $demo with vars:"
echo "Number of nodes: " $NUM_NODES
echo "Opendaylight Controller: " $ODL
echo "Base subnet: " $SUBNET

for i in `seq 1 $NUM_NODES`; do
  hostname="gbpsfc"$i
  echo $hostname
  vagrant ssh $hostname -c "sudo -E /vagrant/infrastructure_launch.py"
done

echo "Configuring controller..."
./$demo/rest.py

echo "Post-controller configuration..."
for i in `seq 1 $NUM_NODES`; do
  hostname="gbpsfc"$i
  echo $hostname
  cp $demo/get-nsps.py .
  vagrant ssh $hostname -c "sudo -E /vagrant/get-nsps.py"
done
echo "$demo" > demo.lock

