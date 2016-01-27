./unstack.sh --all

 sudo service rabbitmq-server restart
 sudo service mysql restart

sudo ovs-vsctl set-manager tcp:$ODL:6640
sudo ovs-vsctl add-br br-int
sudo ovs-vsctl set-controller tcp:$ODL:6653

echo "Removing all logs to save space..."
sudo rm -rf /opt/stack/logs/*
sudo rm -rf /home/vagrant/stack.sh.log.*

time ./stack.sh
echo "Finished stacking at: "; date

