#!/usr/bin/env bash

if [ -f "/home/vagrant/sfc03.lock" ]; then
    echo "You have already run sfc03"
    exit
fi
echo "writing lock file /home/vagrant/sfc03.lock"
touch /home/vagrant/sfc03.lock

echo "Creating chain 'mychain' under heat/service:"
source openrc heat service

tacker sfc-create --name mychain --chain testVNF1 --symmetrical True
tacker sfc-show mychain


echo "Can verify flows by 'dumpflows.sh | grep nsp' on devstack-compute-1 and lack of them on devstack-control. Then run sfc04 to add chain to policy."
