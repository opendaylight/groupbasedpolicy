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

    conf_switches = []
    if args.local:
        for switch in switches:
            if switch['name'] == args.local:
                conf_switches = [switch]
                break
 
    net = None
    if len(conf_switches) > 0:
        net = mininet_gbp.startMininet(conf_switches, hosts, args.controller)
    try :
        if args.policy:
            for switch in switches:
                odl_gbp.get_node_config(switch['dpid'], switch['tunnelIp'])
                
            odl_gbp.register_nodes(args.controller)

        tenant = odl_gbp.get_tenant(TENANT)
        odl_gbp.get_l3c(TENANT, L3CTX)
        odl_gbp.get_bd(TENANT, L2BD, L3CTX)
       
        subnets = {}
        fds = {}
        for host in hosts:
            if args.local and host['switch'] != args.local:
                continue
            nw = ipaddr.IPv4Network(host['ip'])
            snet = "{}/{}".format(nw.network + 1, nw.prefixlen)
            router = "{}".format(nw.network + 1)
        
            if snet not in subnets:
                 snid = str(uuid.uuid4())
                 fdid = str(uuid.uuid4())
                 fds[fdid] = odl_gbp.get_fd(TENANT, fdid, L2BD)
        
                 subnets[snet] = odl_gbp.get_subnet(TENANT, snid, fdid, snet, router)
                 odl_gbp.get_epg(TENANT, host['endpointGroup'])["network-domain"] = snid
        
            odl_gbp.get_ep(TENANT, 
                           host['endpointGroup'], 
                           L3CTX, 
                           re.sub(r'/\d+$', '', host['ip']),
                           L2BD,
                           host['mac'], 
                           int(net.get(host['switch']).dpid), host['port'])
        
        for contract in contracts:
             odl_gbp.get_contract(TENANT, 
                          contract['provider'], contract['consumer'], 
                          contract['id'])
        
        if args.policy:
            odl_gbp.register_tenants(args.controller)

        odl_gbp.register_eps(args.controller)

        if net is not None:
            mininet.cli.CLI(net)
    finally:
        if net is not None:
            net.stop()
