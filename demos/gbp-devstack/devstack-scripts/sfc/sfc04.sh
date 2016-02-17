#!/usr/bin/env bash

if [ -f "/home/vagrant/sfc04.lock" ]; then
    echo "You have already run sfc04"
    exit
fi
echo "writing lock file /home/vagrant/sfc04.lock"
touch /home/vagrant/sfc04.lock

neutron security-group-rule-list

echo 'Select a security-group-rule UUID from the above list to apply a chain to. This step can be repeated to add more security group rules to the chain.

You can enter this either via the YangUI or sending:

http://localhost:8181/restconf/operations/neutron-gbp-mapper:change-action-of-security-group-rules
{
    "input": {
        "security-group-rule": [
            {
                "uuid": "<uuid of rule1>"
            },
            {
                "uuid": "<uuid of rule2>"
            }
        ],
        "action": {
            "sfc-chain-name": "mychain"
        }
    }
}'

#python /vagrant/devstack-scripts/sfc/chain.py $ODL client_sg web_sg mychain

echo "Chain action added to policy. 

Enter route for SF on devstack-compute-1:

kernelmods.sh
"
