#! /bin/bash

#install vpp
echo "Installing VPP..."
sudo apt-get update --allow-unauthenticated
sudo apt-get -y -f install --allow-unauthenticated
echo "resetting hugepages..."
sudo sysctl -w vm.nr_hugepages=0
sleep 3
sudo sysctl -w vm.nr_hugepages=512
HUGEPAGES=`sysctl -n  vm.nr_hugepages`
if (($HUGEPAGES < 512 )); then
    echo "!!!!!!!!!!!!ERROR: Unable to get 512 hugepages, only got $HUGEPAGES.  Cannot finish!!!!!!!!!!!!"
    exit
fi
sudo apt-get install -y --allow-unauthenticated vpp-lib vpp vpp-dev vpp-plugins vpp-dpdk-dkms
sudo sed -i 's/vm.nr_hugepages=1024/vm.nr_hugepages=512/' /etc/sysctl.d/80-vpp.conf
sudo sed -i 's/vm.max_map_count=3096/vm.max_map_count=1200/' /etc/sysctl.d/80-vpp.conf
sudo sed -i 's/kernel.shmmax=2147483648/kernel.shmmax=1073741824/' /etc/sysctl.d/80-vpp.conf
echo "Installing VPP done."
#sudo service vpp start