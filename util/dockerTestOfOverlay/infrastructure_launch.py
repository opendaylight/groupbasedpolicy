#!/usr/bin/python

import re
import time
import sys
import ipaddr
from subprocess import call
from subprocess import check_output
from infrastructure_config import *

def addController(sw, ip):
    call(['ovs-vsctl', 'set-controller', sw, 'tcp:%s:6653' % ip ])

def addSwitch(name, dpid=None):
    call(['ovs-vsctl', 'add-br', name]) #Add bridge
    if dpid:
        if len(dpid) < 16: #DPID must be 16-bytes in later versions of OVS
            filler='0000000000000000'
            dpid=filler[:len(filler)-len(dpid)]+dpid
        elif len(dpid) > 16:
            print 'DPID: %s is too long' % dpid
            sys.exit(3)
        call(['ovs-vsctl','set','bridge', name,'other-config:datapath-id=%s'%dpid])

def addHost(net, switch, name, ip, mac):
    containerID=launchContainer()

def setOFVersion(sw, version='OpenFlow13,OpenFlow12,OpenFlow10'):
    call(['ovs-vsctl', 'set', 'bridge', sw, 'protocols={}'.format(version)])

def addTunnel(sw, sourceIp=None):
    ifaceName = '{}_vxlan0'.format(sw)
    cmd = ['ovs-vsctl', 'add-port', sw, ifaceName,
           '--', 'set', 'Interface', ifaceName,
           'type=vxlan',
           'options:remote_ip=flow',
           'options:key=flow']
    if sourceIp is not None:
        cmd.append('options:source_ip={}'.format(sourceIp))
    call(cmd)

def launchContainer(host,containerImage):
    containerID= check_output(['docker','run','-d','--net=none','--name=%s'%host['name'],'-h',host['name'],'-t', '-i','--privileged=True',containerImage,'/bin/bash']) #docker run -d --net=none --name={name} -h {name} -t -i {image} /bin/bash
    return containerID[:-1] #Remove extraneous \n from output of above

def connectContainerToSwitch(sw,host,containerID,of_port):
    hostIP=host['ip']
    mac=host['mac']
    nw = ipaddr.IPv4Network(hostIP)
    broadcast = "{}".format(nw.broadcast)
    router = "{}".format(nw.network + 1)
    cmd=['./ovswork.sh',sw,containerID,hostIP,broadcast,router,mac,of_port]
    if host.has_key('vlan'):
        cmd.append(host['vlan'])
    call(cmd)


def launch(switches, hosts, contIP='127.0.0.1'):

    for sw in switches:
        dpid=sw['dpid']
        addSwitch(sw['name'],sw['dpid'])
        addTunnel(sw['name'], sw['tunnelIp'])

        ports=0
        for host in hosts:
            if host['switch'] == sw['name']:
                ports+=1
                containerImage=defaultContainerImage #from Config
                if host.has_key('container_image'): #from Config
                    containerImage=host['container_image']
                containerID=launchContainer(host,containerImage)
                connectContainerToSwitch(sw['name'],host,containerID,str(ports))
                host['port']=str(ports) # alagalah - this is such a horrible hack TODO: Find a more elegant way


        # This is a workaround for a bug encountered during
        # the Helium release. Setting the vSwitch from 1.0
        # to 1.3 while it was connected to the controller
        # exposed a bug in the openflowplugin, which resulted
        # in the controller missing some of the ports on the
        # vswitch. This change avoids the bug by switching 
        # the version before connecting the switch to the
        # controller.
        setOFVersion(sw['name'])
        addController(sw['name'], contIP)
        
    return dpid

