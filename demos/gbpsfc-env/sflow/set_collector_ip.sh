#!/usr/bin/env bash

source /vagrant/env.sh
source /vagrant/sflow/settings.sh
export THIS_IP=$(ifconfig | grep -A 1 'eth2' | tail -1 | cut -d ':' -f 2 | cut -d ' ' -f 1)

echo "#!/usr/bin/env bash" >/vagrant/sflow/curl_put_collector.sh
echo "curl -H \"Content-Type:application/yang.data+json\" -X PUT --data \"{'ofoverlay:of-overlay-config': {'sflow-client-settings': {'gbp-ofoverlay-sflow-retrieve-interval': $SFLOW_INTERVAL, 'gbp-ofoverlay-sflow-collector-uri': 'http://$THIS_IP:8008'}}}\" http://admin:admin@$ODL:8181/restconf/config/ofoverlay:of-overlay-config" >>/vagrant/sflow/curl_put_collector.sh

sed -i "/export COLLECTOR_IP=/c export COLLECTOR_IP=$THIS_IP" /vagrant/sflow/internal_settings.sh
