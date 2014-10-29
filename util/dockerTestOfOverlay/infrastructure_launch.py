import re
import time
import sys
import ipaddr
from subprocess import call
from subprocess import check_output
from config import *

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
#    return switch

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
                host['port']='openflow%s:%s' %(str(sw['dpid']),str(ports)) # alagalah - this is such a horrible hack TODO: Find a more elegant way

        # ODL is very fragile so let's give it some time
#        time.sleep(1)
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
# '''
# Created on Oct 16, 2014
# 
# @author: alagalah
# '''
# 
# import infrastructure_config
# import os
# import sys
# import argparse
# import io
# import subprocess
# import ipaddr
# 
# 
# if __name__ == '__main__':
#     '''
#     Usage:
#         1. Assumes well formed infrastructure_config.py file to build containers etc
#         2. If used with --dockerfiles_only True, only the startup scripts and docker files will
#         be created, without building an image or launching the containers.
#         3. If used with --directory {directory} creates all files in directory
#     '''
#     DEBUG=False
# 
#     #Check for parameters to see if --dockerfiles_only True is passed
#     parser = argparse.ArgumentParser()
#     parser.add_argument('--dockerfiles_only', help='Does not launch containers if set to True',default=False)
#     parser.add_argument('--directory', help='Base directory to create dockerfiles and shellscripts',default='.')
#     args = parser.parse_args()
# 
#     LAUNCH=True
#     if (args.dockerfiles_only):
#         LAUNCH=False
# 
#     if (args.directory):
#         os.chdir(args.directory)
#         print 'Working directory: ',os.getcwd()
# 
#     WORKING_DIRECTORY=os.getcwd()
# 
#     #===========================================================================
#     # For each container, the following steps are executed:
#     #    1. Create a shell script locally that will run inside each container.
#     #    It is called 'startup-{switchname}.sh that:
#     #        - Stops, cleans, starts OVS
#     #        - Executes ovs-vsctl commands to create ports and tunnels
#     #        - Assigns IP addresses to container interfaces
#     #        - Fires off a bash script
#     #    2. Create a Dockerfile that:
#     #        - Leverages the base image from infrastructure_config (FROM:)
#     #        - ADD startup-{switchname}.sh which copies file from local to container
#     #        - RUN chmod +x startup-{switchname}.sh
#     #        - CMD ./startup-{switchname}.sh
#     #    3. Build a docker image called {switchname} using the Dockerfile-{switchname}.
#     #    4. Run the docker image with flags '-i -t -d --privileged=True'
#     #
#     #===========================================================================
#     docker_commands=[]
#     for container in infrastructure_config.containers:
#         docker_commands_container=[]
#         DOCKERIMAGE_NAME=str(container['name']).lower()
#         SCRIPT_NAME='startup-'+DOCKERIMAGE_NAME+'.sh'
#         #DOCKERFILE_NAME='Dockerfile-'+DOCKERIMAGE_NAME
#         DOCKERFILE_NAME='Dockerfile'
# 
#         if DEBUG: print "DEBUG: Processing container ",DOCKERIMAGE_NAME, SCRIPT_NAME, DOCKERIMAGE_NAME
# 
#         # Create shell script file to execute following
#         shell_lines=[]
# 
#         shell_lines.append(" ovsdb-server --remote=punix:/usr/local/var/run/openvswitch/db.sock --remote=db:Open_vSwitch,Open_vSwitch,manager_options --pidfile --detach")
#         shell_lines.append("ovs-vswitchd --pidfile --detach")
# #         shell_lines.append('service openvswitch-switch stop')   #Kill OVS process if running (shouldn't be)
# #         shell_lines.append('rm /etc/openvswitch/conf.db')       #Remove any config hanging around
# #         shell_lines.append('service openvswitch-switch start')  #Restart all fresh and squeaky clean
#         shell_lines.append('/bin/sh export PS1="'+DOCKERIMAGE_NAME+'"')   #Set the prompt to the switchname for usability
#         shell_lines.append('ovs-vsctl add-br '+DOCKERIMAGE_NAME)   #Add the bridge
#         shell_lines.append('ovs-vsctl set bridge %s other-config:datapath-id=%s' % (DOCKERIMAGE_NAME,container["dpid"]))   #Set DPID
# #         shell_lines.append('ovs-vsctl set bridge %s protocols="OpenFlow13,OpenFlow10"' % DOCKERIMAGE_NAME)   #Set OF13
# #         shell_lines.append('ovs-vsctl set bridge %s datapath_type=netdev '+DOCKERIMAGE_NAME)   #Alagalah - experimental
#         shell_lines.append('ovs-vsctl set bridge %s protocols="OpenFlow13"' % DOCKERIMAGE_NAME)   #Set OF13
# 
# 
#         # Adding internal ports
#         for hostport in container["host_ports"]:
#             if DEBUG: print "DEBUG: Processing port: ",hostport["port_name"],hostport.keys()
# 
# 
#             port_name=str(hostport["port_name"])
#             port_ip=str(hostport["port_ip"])
#             port_ip_mask=str(hostport["port_ip_mask"])
#             port_mac=str(hostport["port_mac"])
# 
#             shell_lines.append('ovs-vsctl add-port %s %s -- set interface %s type=internal' % (DOCKERIMAGE_NAME, port_name, port_name)) #Add hostport to switch as internal hostport
#             shell_lines.append('ifconfig '+port_name+' down') #Down it
#             shell_lines.append('ifconfig '+port_name+' hw ether '+port_mac) #Set the MAC address
#             shell_lines.append('ifconfig '+port_name+' '+port_ip+' netmask '+port_ip_mask) #Set the IP address
#             shell_lines.append('ifconfig '+port_name+' up') #Up it
# 
#         #Reset the docker default interface address which is Eth0
# #TODO: Perhaps look to add pipeworks functionality of a bridge/hostport just for POC/Demos
#         shell_lines.append('ifconfig eth0 down') #Down it
#         shell_lines.append('ifconfig eth0 hw ether '+str(container["host_interface_mac"])) #Set the MAC address
#         shell_lines.append('ifconfig eth0 ' + str(container["ip_address"])+' netmask '+str(container["ip_mask"])) #Set the IP address
#         shell_lines.append('ifconfig eth0 up') #Up it
# 
# 
#         # Adding tunnel ports
#         for tunnel in container["tunnels"]:
#             tunnel_type=tunnel["type"] #only supported at moment, may look into ivxlan and GRE later
#             port_name=tunnel["port_name"]
#             key=tunnel["key"] #allows us to overload VXLAN tunnel using VNIs if needed if using flow
#             remote_ip=tunnel["remote_ip"]
#             openflow_port=tunnel["openflow_port"] #creates an OF port in datapath to map flows to tunnel
# 
# #             #Set using remote tunnel destination
# #             shell_lines.append('ovs-vsctl add-port %s %s -- set interface %s type=%s option:remote_ip=%s option:key=%s ofport_request=%s' %
# #                                (DOCKERIMAGE_NAME,port_name,port_name,tunnel_type,remote_ip,key,openflow_port))
# 
# # Setting setting source tunnel only, no OFPORT REQUEST
#             shell_lines.append('ovs-vsctl add-port %s %s -- set interface %s type=%s option:remote_ip=flow option:key=%s option:source_ip=%s' %
#                                (DOCKERIMAGE_NAME,port_name,port_name,tunnel_type,key,container["ip_address"]))
# 
#         #####################
#         # WARNING! THIS IS A BIT HACKY! UNTIL CONFIG CHANGES TO USE PIPEWORKS THIS SHOULD BE OK --- REALLY RELIES ON SINGLE PORT!!!  OR LAST PORT SET!
#         shell_lines.append('ip route add %s via 172.17.42.1' % infrastructure_config.OPENDAYLIGHT_CONTROLLER_IP)
#         nw = ipaddr.IPv4Network(port_ip+"/"+port_ip_mask)
#         snet = "{}/{}".format(nw.network + 1, nw.prefixlen)
#         router = "{}".format(nw.network + 1)
#         shell_lines.append('ip route default via %s' % router) #alagalah HEINOUS
# 
#         # END WARNING
#         #####################
#         # Want to register with the controller last.
#         shell_lines.append('ovs-vsctl set-controller %s tcp:%s:%s' % (DOCKERIMAGE_NAME,infrastructure_config.OPENDAYLIGHT_CONTROLLER_IP,infrastructure_config.OPENDAYLIGHT_CONTROLLER_PORT))   #Set the CONTROLLER
# #        shell_lines.append('ovs-vsctl set-manager %s tcp:%s:%s' % (DOCKERIMAGE_NAME,infrastructure_config.OPENDAYLIGHT_CONTROLLER_IP,"6640"))   #Set the CONTROLLER
# 
#         shell_lines.append('/bin/bash') #Leave a bash shell running else it dies... could also do "tail /dev/null"
# 
#         #Create the shell script
#         #These scripts only work correctly if docker has its own folder per container
#         directory=os.path.join(WORKING_DIRECTORY,DOCKERIMAGE_NAME)
#         if not os.path.exists(directory):
#             os.makedirs(directory)
#         os.chdir(directory)
#         with open(SCRIPT_NAME, 'w') as f:
#             for s in shell_lines:
#                 f.write(s + '\n')
#         print "Created script ",SCRIPT_NAME
# 
#         #===============================================================================
#         # Step 1 COMPLETE, NOW FOR Step 2 - Creating the Docker file
#         #===============================================================================
# 
#         dockerfile_lines=[]
# 
#         dockerfile_lines.append("FROM %s" % container["image"])
#         dockerfile_lines.append("ADD %s /" % SCRIPT_NAME)
#         dockerfile_lines.append("RUN chmod +x %s" % SCRIPT_NAME)
#         dockerfile_lines.append("CMD ./%s" % SCRIPT_NAME)
# 
#         #Create the Dockerfile
#         with open(DOCKERFILE_NAME, 'w') as f:
#             for s in dockerfile_lines:
#                 f.write(s + '\n')
#         print "Created docker file ",DOCKERFILE_NAME
# 
#         #=======================================================================
#         # Steps 4 & 5 create the docker CLI commands to BUILD (using the Dockerfile)
#         # and RUN the image, which automatically runs the startup-{switch}.sh script
#         #=======================================================================
#         docker_commands_container.append(os.getcwd()) # This is how we know what directory to go to
#         docker_commands_container.append('sudo docker build -t %s .' % DOCKERIMAGE_NAME)
#         docker_commands_container.append('sudo docker run -t -i -d --privileged=true --name=%s %s' % (DOCKERIMAGE_NAME,DOCKERIMAGE_NAME))
#         docker_commands.append(docker_commands_container)
# 
#     # Only execute docker launch commands if --dockerfiles_only is NOT set to True
#     if LAUNCH:
#         for command in docker_commands:
#             print command
#             os.chdir(command[0])
#             print "Changed directory to ",os.getcwd()
#             os.system(command[1])
#             os.system(command[2])
