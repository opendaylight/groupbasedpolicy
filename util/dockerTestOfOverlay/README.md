GroupBasedPolicy Proof Of Concept Scripts

1. Introduction

This instance of GroupBasedPolicy "Proof of Concept" or Demo utilizes:
- Two Ubuntu 14.04 LTS VMs on a host-net (using VirtualBox)
- Docker 1.0.1 (for guests)
- OpenVSwitch 2.3.0 (running natively on Ubuntu 14.04LTS)

It mimics the same policy as the mininet example, that is:
- Two EndPointGroups (EPG)
  1. Clients
  2. WebServers
- 4 guests per EPG
- Contract allowing HTTP Client -> WebServers and PING Client <-> WebServers but
  HTTP WebServers -//-> Client (ie disallowed)

  2. Files
  - infrastructure_config:
      Contains configuration of OVS and docker guests. There is a default image, but this can be over-ridden.
  - policy_config:
      The policy is set here. It is important to note that this file needs to be on both VMs
  - infrastructure_launch:
      Launches the docker containers and configures switches
  - odl_gbp:
      Library of functions for performing operations on GBP policy entities
  - testOfOverlay:
      Processes policy and guests. It is important that one of the VMs launches this script with "--policy" much like the mininet POC
  - start-poc.sh:
      Cleans up any existing docker and OVS instances (using "mn -c" for quick OVS clean up. If this is not an option on your VM then pursue stopping OVS, removing conf.db, starting OVS)

  3. Usage
  - Always run from root. ie sudo bash
  - Edit infrastructure_config.py with the IP address of the VM for each switch, edit start-poc.sh with your ODL controller IP.

  
