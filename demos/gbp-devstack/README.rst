devstack-nodes
==============

This repo provides a Vagrantfile with provisioning that one can use to easily
get a cluster of nodes configured with DevStack.

It is a fork of the wonderful work of Mr Flavio Fernandes

Usage
-----

1) Run VMs::
    
A Vagrantfile is provided to easily create a DevStack environment to test with. To save
performance, it is sufficient to run all the required services just on one VM. This VM
is identified as control node. Other VMs are compute nodes. First, set number of compute
nodes desired by setting::
   'DEVSTACK_NUM_COMPUTE_NODES=1'
    
Note: Only 3 or less nodes are supported today.


Next, execute::

    vagrant up
    
If no VMs have been generated yet, they will be now.


    
2) Start devstack::

    vagrant ssh [devstack-control|devstack-compute-1]

    cd devstack
    
To make devstack-scripts visible::

    sudo cp /vagrant/devstack-scripts/environment /etc/environment

    source /etc/environment

    sudo ovs-vsctl add-br br-int

    ^ one time only.

   
This assumes that ODL is 192.168.50.1. If you need to change this, edit /etc/environment,
change the 'export ODL=' to the right IP address, save, exit, and repeat source command above.
 
After stacking for the first time, edit local.conf and:
	
uncomment: 'OFFLINE=True'

comment out: 'RECLONE=yes'

To stack safely, from $HOME/devstack directory on all the nodes execute::

    restack.sh

   
Note: NOT ./restack.sh ... just restack.sh ... its in the PATH.
 
To verify from control node if all the nodes are stacked successfully::

    source openrc admin admin

    nova hypervisor-list


Testing
-----

1) Check the ovs bridges first::

    sudo ovs-vsctl show


2) Run scripts from ~/devstack/ directory. These scripts are in the path. If you need to modify them,
   they are in /vagrant/devstack-scripts/tutorial::

    step01.sh: client node on devstack-control, web node on devstack-compute-1

    step02.sh

    step03.sh


(videos and documentation coming soon).


3. Useful commands to verify::

    flowcount.sh br-int : gives per table flow counts

    flowcount.sh br-int <table#> : dumps flows from <table> in priority order


4. You can point your browser at::
  
    Horizon: 192.168.50.20 (u: admin, p:admin).

    This assumes you are familiar with Horizon. Ensure you look at the admin project.

    ODL GBP GUI: 192.168.50.1:8181/index.html (u: admin, p: admin).

    See GBP UserGuide for more information on using the GUI in Stable/Lithium.


