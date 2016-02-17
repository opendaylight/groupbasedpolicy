sudo ovs-vsctl del-controller br-int
sleep 6
sudo ovs-vsctl set-controller br-int tcp:$ODL:6653

