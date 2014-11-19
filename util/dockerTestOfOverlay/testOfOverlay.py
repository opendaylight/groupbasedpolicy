#!/usr/bin/python

import infrastructure_launch
import odl_gbp
import ipaddr
import uuid
import re
import argparse, sys
import policy_config
import infrastructure_config

def getSubnet(ip):
    nw = ipaddr.IPv4Network(ip)
    return "{}/{}".format(nw.network + 1, nw.prefixlen)

if __name__ == '__main__':

    # Validate all parameters are present
    parser = argparse.ArgumentParser()
    parser.add_argument('--local',
                        help='Set up distributed mininet on local host with the specified switch')
    parser.add_argument('--policy', action='store_true',
                        help='Configure the policy on the controller')
    parser.add_argument('--controller', default='127.0.0.1',
                        help='Use the specified controller IP address')
    args = parser.parse_args()

    if (not args.local and not args.policy):
        parser.print_help()
        sys.exit(3)

    # switches is a list from infrastructure_config.py, these are the OVS instances
    conf_switches = []
    if args.local:
        for switch in infrastructure_config.switches:
            if switch['name'] == args.local:
                conf_switches = [switch]
                break

    # Assuming we have switches defined (and hence conf_switches), start containers with the "hosts" list also from infrastructure_config.py
    if len(conf_switches) > 0:
        dpid=infrastructure_launch.launch(conf_switches, infrastructure_config.hosts, args.controller)

    if args.policy:
        for switch in infrastructure_config.switches:
            # This leverages a global from odl_gbp called "nodes", which appends "data" from this for loop
            odl_gbp.get_node_config(switch['dpid'], switch['tunnelIp'])
        #This also uses the global "nodes" from odl_gbp
        odl_gbp.register_nodes(args.controller)

    #Only one tenant supported today
    tenant = policy_config.tenants[0]
    tenant = odl_gbp.initialize_tenant(tenant)
    if len(tenant['l3-context']) ==0:
        print "Setting L3 context"
        odl_gbp.get_l3c(tenant['id'], policy_config.L3CTX)
    l3context=tenant['l3-context'][0]['id']
    if len(tenant['l2-bridge-domain']) == 0:
        print "Setting L2 Bridge domain"
        odl_gbp.get_bd(tenant['id'], policy_config.L2BD, tenant['l3-context'][0]['id'])
    l2bridgeDomain=tenant['l2-bridge-domain'][0]['id']
    # subnets and fds (flood domains)
    subnets = {}
    fds = {}
    # hosts comes from infrastructure_config.py, which contains target switch, IP Address, MAC address, tenant and EPG
    for host in infrastructure_config.hosts:
        if args.local and host['switch'] != args.local:
            continue
        nw = ipaddr.IPv4Network(host['ip'])
        snet = "{}/{}".format(nw.network + 1, nw.prefixlen)
        router = "{}".format(nw.network + 1)

        if snet not in subnets:
            snid = str(uuid.uuid4())
            fdid = str(uuid.uuid4())
            # Sets flood domain where parent is L2BD from config.py
            fds[fdid] = odl_gbp.get_fd(tenant['id'], fdid, l2bridgeDomain)

            # sets subnet from tenant, which also includes the flood domain
            subnets[snet] = odl_gbp.get_subnet(tenant['id'], snid, fdid, snet, router)
            # Sets the "network-domain" in global endpointGroups dict in odl_gbp.py

            for endpointGroup in policy_config.endpointGroups:
                if host['endpointGroup'] == endpointGroup['name']:
                    groupId=endpointGroup['id']
            odl_gbp.get_epg(tenant['id'], groupId)["network-domain"] = snid

        # Creates EP information and appends to endpoint list, a global
        odl_gbp.get_ep(tenant['id'], 
                       groupId, 
                       l3context, 
                       re.sub(r'/\d+$', '', host['ip']),
                       l2bridgeDomain,
                       host['mac'],
                       dpid, 
                       host['port'])

    # Resolve contract names to IDs and add to policy
    contractConsumerEpgIDs=[]
    contractProviderEpgIDs=[]
    for contract in policy_config.contracts:
        for endpointGroup in policy_config.endpointGroups:
            if contract['name'] in endpointGroup['consumesContracts']:
                contractConsumerEpgIDs.append(endpointGroup['id'])
            if contract['name'] in endpointGroup['providesContracts']:
                contractProviderEpgIDs.append(endpointGroup['id'])
        for contractProviderEpgID in contractProviderEpgIDs:
            for contractConsumerEpgID in contractConsumerEpgIDs:
                odl_gbp.get_contract(tenant['id'],
                              contractProviderEpgID,
                              contractConsumerEpgID,
                              contract)

    # POST to the controller to register tenants
    if args.policy:
        odl_gbp.register_tenants(args.controller)

    # POST to controller to register EPS
    odl_gbp.register_eps(args.controller)

