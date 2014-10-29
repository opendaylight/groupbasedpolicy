#===============================================================================
# Containers are created from the config below. Basic structure is:
#    Container
#    - basic container information such as it's IP it uses to talk to host etc
#    - host ports: these are EPs in the policy_config.py file. Processes such as
#      a simple socket server, HTTPD, database etc can be run in the container.
#    - tunnel ports are what make up the topology, currently the remote_ip points
#      to the docker container IP_address
#===============================================================================

OPENDAYLIGHT_CONTROLLER_IP="192.168.56.1"
OPENDAYLIGHT_CONTROLLER_PORT=6653

#TODO: modify script to leverage pipework for multiple host bridges/remote systems, as well as making 172.17.0.0/16 configurable
#TODO: change remote IP to point to another container by container "name" and resolve it's IP Address
#TODO: Write a GUI that can instantiate these values.
#TODO: Change script to automatically pull image from docker repo.

containers = [{
              "name" : "s1", #synonymous with switch name
              "image" : "alagalah/odlpoc_ovs230",
              "ip_address" : "172.17.0.101", # IP address of the switch and relies on docker default of 172.17.0.0/16.
              "ip_mask" : "255.255.0.0",
              "host_interface_mac" : "00:00:00:fa:bb:01",
              "dpid" : "0000000000000001", # Must be 16 "bytes" long
              "host_ports" : [
                          {"port_name" : "p1", # synonymous with EP
                           "port_ip" : "10.1.1.11", # synonymous with EP
                           "port_ip_mask" :"255.255.0.0",
                           "port_mac" : "de:ad:10:01:01:11",
                           "vlan" : None}
#                           {"port_name" : "p2", # synonymous with EP
#                            "port_ip" : "30.1.1.11", # synonymous with EP
#                            "port_ip_mask" :"255.255.0.0",
#                            "port_mac" : "de:ad:30:01:01:11",
#                            "vlan" : None}
                              ],
               "tunnels" : [
                            {"type" :"vxlan", #only vxlan supported at moment, may look into ivxlan and GRE later
                             "port_name" : "s1_vxlan1", #"vxlan1" is just a string and can be anything you like
                             "key" :"flow", #allows us to overload VXLAN tunnel using VNIs if needed
                             "openflow_port" : "10", #creates an OF port in datapath to map flows to tunnel
                             "remote_ip" : "172.17.0.103" #Optional... TODO firx it.
                             }
                            ]
              },
              {"name" : "s2", 
              "image" : "alagalah/odlpoc_ovs230",
              "ip_address" : "172.17.0.102",
              "ip_mask" : "255.255.0.0",
              "host_interface_mac" : "00:00:00:fa:bb:02",
              "dpid" : "0000000000000002",
              "host_ports" : [
                          {"port_name" : "p1", 
                           "port_ip" : "20.1.1.11",
                           "port_ip_mask" :"255.255.0.0",
                           "port_mac" : "de:ad:20:01:01:11",
                           "vlan" : None}
#                           {"port_name" : "p2", 
#                            "port_ip" : "20.1.1.12",
#                            "port_ip_mask" :"255.255.0.0",
#                            "port_mac" : "de:ad:20:01:01:12",
#                            "vlan" : None}
                              ],
               "tunnels" : [
                            {"type" :"vxlan",
                             "port_name" : "s2_vxlan1",
                             "key" :"flow", 
                             "openflow_port" : "10",
                             "remote_ip" : "172.17.0.101"
                             }
                            ]
              }] #end containers
