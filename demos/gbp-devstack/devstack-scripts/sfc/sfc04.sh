#!/usr/bin/env bash

if [ -f "/home/vagrant/sfc04.lock" ]; then
    echo "You have already run sfc04"
    exit
fi
echo "writing lock file /home/vagrant/sfc04.lock"
touch /home/vagrant/sfc04.lock

python /vagrant/devstack-scripts/sfc/chain.py $ODL client_sg web_sg mychain

echo "Chain action added to policy. 

kernelmods.sh
"
