#!/usr/bin/env bash

. /vagrant/sflow/internal_settings.sh
hostnum=${HOSTNAME#"gbpsfc"}
sw="sw$hostnum"

if [ -f ~/sflow_uuid ]; then
  echo "sflow_uuid already present; cleaning..."
  sudo ovs-vsctl remove bridge $sw sflow `cat ~/sflow_uuid`
  rm ~/sflow_uuid
fi

echo "Starting sflow..."
sudo ovs-vsctl -- --id=@sflow create sflow agent=${AGENT_IP} \
target=\"${COLLECTOR_IP}:${COLLECTOR_PORT}\" header=${HEADER_BYTES} \
sampling=${SAMPLING_N} polling=${POLLING_SECS} -- set bridge $sw sflow=@sflow >~/sflow_uuid && cat ~/sflow_uuid
