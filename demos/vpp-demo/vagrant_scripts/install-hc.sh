#! /bin/bash

#install hc
echo "Installing Honeycomb..."
sudo apt-get update --allow-unauthenticated
sudo apt-get -y -f install --allow-unauthenticated
sudo apt-get -qq install -y --allow-unauthenticated honeycomb
sudo sed -i 's/"persist-context": "true"/"persist-context": "false"/g' /opt/honeycomb/config/honeycomb.json
sudo sed -i 's/"persist-config": "true"/"persist-config": "false"/g' /opt/honeycomb/config/honeycomb.json
sudo sed -i 's/"127.0.0.1"/"0.0.0.0"/g' /opt/honeycomb/config/honeycomb.json
sudo sed -i 's/"restconf-port": 8183/"restconf-port": 8283/g' /opt/honeycomb/config/honeycomb.json
echo "Installing Honeycomb done."
#sudo service honeycomb start