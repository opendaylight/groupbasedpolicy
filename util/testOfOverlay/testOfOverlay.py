#!/usr/bin/python

import mininet_gbp
import odl_gbp
import mininet.cli
import ipaddr
import uuid
import re
import argparse, sys
from config import *

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

    # switches is a list from config.py, when this script is called with --local (switch) and its present in config, it is added to the conf_switches
    conf_switches = []
    if args.local:
        for switch in switches:
            if switch['name'] == args.local:
                conf_switches = [switch]
                break

    # Assuming we have switches defined (and hence conf_switches), start mininet with the "hosts" list also from config.py
    net = None
    if len(conf_switches) > 0:
        net = mininet_gbp.startMininet(conf_switches, hosts, args.controller)
    try :
        if args.policy:
            for switch in switches:
                # This leverages a global from odl_gbp called "nodes", which appends "data" from this for loop
                odl_gbp.get_node_config(switch['dpid'], switch['tunnelIp'])
            #This also uses the global "nodes" from odl_gbp
            odl_gbp.register_nodes(args.controller)

        # TENANT, L3CTX, L2BD are imported from config.py
        # get_tenant looks for the TENANT UUID in a global tenant dictionary in odl_gbp.
        # If TENANT doesn't already exist in that dict. then a bunch of 'default' tenant data is defined, inluding
        # subjects and classifiers (at writing specific to HTTP source/dest and ICMP)
        tenant = odl_gbp.get_tenant(TENANT)

        # Layer3 context and Layer BridgeDomain are SET into the tenant{} structure in odl_gbp 
        # TODO: (maybe call these set???)
        odl_gbp.get_l3c(TENANT, L3CTX)
        odl_gbp.get_bd(TENANT, L2BD, L3CTX)

       # subnets and fds (flood domains)
        subnets = {}
        fds = {}
        # hosts comes from config.py, which contains target switch, IP Address, MAC address, tenant and EPG
        for host in hosts:
            # ??????
            if args.local and host['switch'] != args.local:
                continue
            nw = ipaddr.IPv4Network(host['ip'])
            snet = "{}/{}".format(nw.network + 1, nw.prefixlen)
            router = "{}".format(nw.network + 1)

            if snet not in subnets:
                 snid = str(uuid.uuid4())
                 fdid = str(uuid.uuid4())
                 # Sets flood domain where parent is L2BD from config.py
                 fds[fdid] = odl_gbp.get_fd(TENANT, fdid, L2BD)

                 # sets subnet from tenant, which also includes the flood domain
                 subnets[snet] = odl_gbp.get_subnet(TENANT, snid, fdid, snet, router)

                 # Sets the "network-domain" in global endpointGroups dict in odl_gbp.py
                 odl_gbp.get_epg(TENANT, host['endpointGroup'])["network-domain"] = snid

            # Creates EP information and appends to endpoint list, a global
            odl_gbp.get_ep(TENANT, 
                           host['endpointGroup'], 
                           L3CTX, 
                           re.sub(r'/\d+$', '', host['ip']),
                           L2BD,
                           host['mac'], 
                           int(net.get(host['switch']).dpid), host['port'])

        # contracts is a global list from config.py.
        # get_contract creates the specific subject, classifiers, rules etc for the contract
        #     and appends this to the global tenant list.
        for contract in contracts:
             odl_gbp.get_contract(TENANT, 
                          contract['provider'], contract['consumer'], 
                          contract['id'])
        
        # POST to the controller to register tenants
        if args.policy:
            odl_gbp.register_tenants(args.controller)

        # POST to controller to register EPS
        # TODO: Should this be done on a per Tenant basis
        odl_gbp.register_eps(args.controller)

        if net is not None:
            mininet.cli.CLI(net)
    finally:
        if net is not None:
            net.stop()
