sudo ovs-vsctl del-manager
sleep 6
sudo ovs-vsctl set-manager tcp:$ODL:6640

